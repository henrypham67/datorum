/*
Copyright 2024.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package controller

import (
	"context"
	"encoding/base64"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/ec2"
	"github.com/aws/aws-sdk-go-v2/service/ec2/types"
	"github.com/go-logr/logr"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/log"

	datorumv1alpha1 "beandev.io/datorum-operator/api/v1alpha1"
	"github.com/aws/aws-sdk-go-v2/config"
)

// DatabaseServerReconciler reconciles a DatabaseServer object
type DatabaseServerReconciler struct {
	client.Client
	Scheme *runtime.Scheme
}

//+kubebuilder:rbac:groups=datorum.beandev.io,resources=databaseservers,verbs=get;list;watch;create;update;patch;delete
//+kubebuilder:rbac:groups=datorum.beandev.io,resources=databaseservers/status,verbs=get;update;patch
//+kubebuilder:rbac:groups=datorum.beandev.io,resources=databaseservers/finalizers,verbs=update

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
// TODO(user): Modify the Reconcile function to compare the state specified by
// the DatabaseServer object against the actual cluster state, and then
// perform operations to make the cluster state reflect the state specified by
// the user.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.17.3/pkg/reconcile
func (r *DatabaseServerReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	l := log.FromContext(ctx)

	// Fetch the DatabaseServer instance
	var db datorumv1alpha1.DatabaseServer
	if err := r.Get(ctx, req.NamespacedName, &db); err != nil {
		l.Error(err, "unable to fetch DatabaseServer")
		return ctrl.Result{}, client.IgnoreNotFound(err)
	}

	AWSConfig, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		l.Error(err, "unable to load SDK config")
		return ctrl.Result{}, err
	}

	EC2Client := ec2.NewFromConfig(AWSConfig)

	if err := r.handleFinalizer(ctx, db); err != nil {
		l.Error(err, "failed to update finalizer")
		return ctrl.Result{}, err
	}

	// Fetch the DatabaseServer instance
	if err := r.Get(ctx, req.NamespacedName, &db); err != nil {
		l.Error(err, "unable to fetch DatabaseServer")
		return ctrl.Result{}, client.IgnoreNotFound(err)
	}

	if !db.ObjectMeta.DeletionTimestamp.IsZero() {
		_, err := EC2Client.TerminateInstances(ctx, &ec2.TerminateInstancesInput{
			InstanceIds: []string{*db.Status.ID},
		})
		// TODO: Drift detection
		if err != nil {
			l.Error(err, "failed to terminate instances")
			return ctrl.Result{}, err
		}
		l.V(1).Info("terminated instance")
	} else if err := r.ensureOrCreateEC2Instance(ctx, EC2Client, &db, l); err != nil {
		// Ensure EC2 instance exists or create one
		return ctrl.Result{}, err
	}

	return ctrl.Result{}, nil
}

type VPC struct {
	ID   string `json:"id"`
	CIDR string `json:"cidr"`
}

func newVPC(ctx context.Context, EC2Client *ec2.Client, l logr.Logger) (*VPC, error) {
	defaultCIDR := "10.0.0.0/16"
	output, err := EC2Client.CreateVpc(ctx, &ec2.CreateVpcInput{
		CidrBlock: aws.String(defaultCIDR),
	})
	if err != nil {
		l.Error(err, "unable to create vpc")
		return nil, err
	}
	l.V(1).Info("created vpc")
	return &VPC{
		ID:   *output.Vpc.VpcId,
		CIDR: *output.Vpc.CidrBlock,
	}, nil
}

type InternetGateway struct {
	ID  string
	VPC *VPC
}

func newIGW(ctx context.Context, EC2Client *ec2.Client, l logr.Logger) (*InternetGateway, error) {
	l.V(1).Info("creating igw")
	output, err := EC2Client.CreateInternetGateway(ctx, &ec2.CreateInternetGatewayInput{})
	if err != nil {
		l.Error(err, "unable to create internet gateway")
		return nil, err
	}
	l.V(1).Info("created igw")
	return &InternetGateway{ID: *output.InternetGateway.InternetGatewayId}, nil
}

func (igw InternetGateway) attachToVPC(vpc *VPC, ctx context.Context, EC2Client *ec2.Client, l logr.Logger) error {
	l.V(1).Info("attaching vpc (%s) to internet gateway (%s)", vpc.ID, igw.ID)
	_, err := EC2Client.AttachInternetGateway(ctx, &ec2.AttachInternetGatewayInput{
		InternetGatewayId: aws.String(igw.ID),
		VpcId:             aws.String(vpc.ID),
	})
	if err != nil {
		l.Error(err, "unable to attach vpc to internet gateway")
		return err
	}
	l.V(1).Info("attached vpc to internet gateway")
	igw.VPC = vpc
	return nil
}

type Subnet struct {
	ID  string
	VPC *VPC
}

func newSubnet(ctx context.Context, EC2Client *ec2.Client, vpc *VPC, l logr.Logger) (*Subnet, error) {
	defaultCIDR := "10.0.0.0/24"
	output, err := EC2Client.CreateSubnet(ctx, &ec2.CreateSubnetInput{
		VpcId:     aws.String(vpc.ID),
		CidrBlock: aws.String(defaultCIDR),
	})
	if err != nil {
		l.Error(err, "unable to create subnet")
		return nil, err
	}
	l.V(1).Info("created subnet")
	return &Subnet{
		ID:  *output.Subnet.SubnetId,
		VPC: vpc,
	}, nil
}

func (r *DatabaseServerReconciler) handleFinalizer(ctx context.Context, db datorumv1alpha1.DatabaseServer) error {
	l := log.FromContext(ctx)
	name := "ec2-deletion"
	if db.ObjectMeta.DeletionTimestamp.IsZero() {
		// add finalizer in case of create/update
		if !controllerutil.ContainsFinalizer(&db, name) {
			ok := controllerutil.AddFinalizer(&db, name)
			l.V(1).Info("Add Finalizer %s : %t", name, ok)
			return r.Update(ctx, &db)
		}
	} else {
		// remove finalizer in case of deletion
		if controllerutil.ContainsFinalizer(&db, name) {
			ok := controllerutil.RemoveFinalizer(&db, name)
			l.V(1).Info("Remove Finalizer %s : %t", name, ok)
			return r.Status().Update(ctx, &db)
		}
	}
	return nil
}

func (r *DatabaseServerReconciler) ensureOrCreateEC2Instance(ctx context.Context, EC2Client *ec2.Client, db *datorumv1alpha1.DatabaseServer, l logr.Logger) error {
	if db.Status.ID == nil {
		return r.createEC2Instance(ctx, EC2Client, db, l)
	}

	output, err := EC2Client.DescribeInstances(ctx, &ec2.DescribeInstancesInput{
		InstanceIds: []string{*db.Status.ID},
	})
	if err != nil {
		l.Error(err, "unable to describe instances")
		return err
	}

	if len(output.Reservations) == 0 {
		return r.createEC2Instance(ctx, EC2Client, db, l)
	}

	return nil
}

func (r *DatabaseServerReconciler) createEC2Instance(ctx context.Context, EC2Client *ec2.Client, db *datorumv1alpha1.DatabaseServer, l logr.Logger) error {
	vpc, err := newVPC(ctx, EC2Client, l)
	if err != nil {
		return err
	}
	subnet, err := newSubnet(ctx, EC2Client, vpc, l)
	if err != nil {
		return err
	}
	igw, err := newIGW(ctx, EC2Client, l)
	if err != nil {
		return err
	}
	err = igw.attachToVPC(vpc, ctx, EC2Client, l)
	if err != nil {
		return err
	}
	userData := `#!/bin/bash
# Update the package list
sudo dnf update

# Install PostgreSQL 13 (or specify another version if needed)
sudo dnf install postgresql15.x86_64 postgresql15-server -y

# Initialize the PostgreSQL database
sudo postgresql-setup --initdb

# Enable PostgreSQL to start on boot and start the service
sudo systemctl start postgresql
sudo systemctl enable postgresql
sudo systemctl status postgresql

# Set up a PostgreSQL superuser with a password
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'admin';"`
	encodedUserdata := base64.StdEncoding.EncodeToString([]byte(userData))
	result, err := EC2Client.RunInstances(ctx, &ec2.RunInstancesInput{
		MaxCount:     aws.Int32(1),
		MinCount:     aws.Int32(1),
		ImageId:      aws.String("ami-02d3770deb1c746ec"),
		InstanceType: types.InstanceTypeT3Nano,
		SubnetId:     aws.String(subnet.ID),
		UserData:     aws.String(encodedUserdata),
	})
	if err != nil {
		l.Error(err, "Could not create instance")
		return err
	}

	db.Status.ID = result.Instances[0].InstanceId
	l.V(1).Info("Created instance", "instance-id", *db.Status.ID)

	if err := r.Status().Update(ctx, db); err != nil {
		l.Error(err, "unable to update DatabaseServer status")
		return err
	}

	return nil
}

// SetupWithManager sets up the controller with the Manager.
func (r *DatabaseServerReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&datorumv1alpha1.DatabaseServer{}).
		Complete(r)
}
