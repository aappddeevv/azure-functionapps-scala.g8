# App

Your awesome azure functionapp functions.

# Background

[azure functionapps](https://docs.microsoft.com/en-us/azure/azure-functions/)
are a way to run functions in response to events. You can build functionapps in
many languages. This project deploys function apps based on scala jvm and scala
javascript execution environments.

* [azure java functions using maven](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-java-maven)
* [java developers guide](https://docs.microsoft.com/en-us/azure/azure-functions/functions-reference-java)
* [java library](https://github.com/Azure/azure-functions-java-library)

## Sub-projects

* helloworldjvmfatjar: helloworld function running on the jvm, one fatjar
* helloworldjvm: helloworld function running on the jvm
* helloworldjs: helloworld function running on node
* common: Common project code if you want to separate them out. This is a shared
  project base that can contain both jvm and js specific code.

The project is setup so there is one function per sub-project. However, a single
sub-project could create all the functions and you could arrange for the
creation of the dist to reflect one jar (say, installed into the root folder)
and multiple function.json, each in their own subdirectory specifying a
different entrypoint. There are a myriad of ways to configure code sharing
within in the project and the deployed functionapp.

## Tasks

All of the tasks are coded within the .sbt file itself and there is no cool
plugin because your packaging needs may be quite diverse and unique *all* the
packaging code are left as tasks. Each project has a few tasks that roughly
follow a naming convention. It's messy but gives you the basis to change
anything you want without going crazy trying to trick sbt into doing what you
want it to do.

* `creatDist`: The output distribution director that consolidates all of the inputs
  either through copying or bundling.
* `createZip`: Create the zip-deploy file suitable for use with curl or azure CLI.
* `upload`: Run the azure CLI if AZURE_RG and AZURE_FUNCTIONAPP_NAME are defined
  in either the props on the environment.
* `buildAndUpload`: Build everything and upload.
* `watchJS`: A command alias that builds *only* the JS project and uploads it.

You may run into issues with bundling the jvm project as creating a single
assembly means that you are combining other assemblies and there may be
conflicts e.g. the same file in the same "path" is included in 2 two different
jvm dependencies.

## Building

First install the javascript side of the application: 

```sh
npm install
```

Then run in sbt:

```sh
sbt
sb> buildAndUpload -DAZURE_RG=<resource group> -DAZURE_FUNCTIONAPP_NAME=<functionapp name> -DBUILD_KIND=prod
```

or 

```sh
AZURE_RG=<resource group> AZURE_FUNCTIONAPP_NAME=<functionapp name> BUILD_KIND=prod sbt
sbt> buildAndUpload
```

The env/props BUILD_KIND (any string starting with prod activates prod build
else dev build) indicates the build kind. If set to "production", the production
webpack bundle will be produced and fullOptJS will be used to build the JS
project.

The default name format for the output zip is `<project name>`. You should also
notice that the jvm artifacts use the standard naming convention from sbt for
the jar's name.

The root project's resources (src/main/resources) will be placed into the
toplevel folder of the zip. For example, you can place the `host.json` file
there (which must be non-empty e.g. "{}") or not include it. zipdeploy is
additive so it only overwrites content in the functionapp storage space that
overlaps with the zip. Because of this, you could, if desired, just build one
function for the zip and deploy that individually.

## Deployment

Deployment can be via
[zipdeploy](https://docs.microsoft.com/en-us/azure/azure-functions/deployment-zip-push)
either with curl or the azure CLI. See the link for details.

The task `upload` uses the azure CLI `az` to upload the zip file if the
env/prop variables have been set.

## Development

There are many ways to do development, one really slow way is to upload the zip
deploy file after each change. The command alias `watchJS` has been created
which does this *only* for the JS project. If you run clean prior to running
this, the upload will only contain changes to the JS project.

A small test payload is available. You can use it via curl by obtaining the function url then:

```sh
curl -X POST <function url> -d @test-payload.json
```

If you do not have any output you can use the curl option `-w '${http_code}` to print the status code.

To restart the functionapp from the command line run:

```sh
az functionapp restart -g $AZURE_RG -n $AZURE_FUNCTIONAPP_NAME
```
