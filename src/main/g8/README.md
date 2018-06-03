# App

Your awesome azure functionapp functions.

# Background

[azure functionapps](https://docs.microsoft.com/en-us/azure/azure-functions/)
are a way to run functions in response to events. You can build functionapps in
many languages. This project deploys function apps based on scala jvm and scala
javascript execution environments.

## Sub-projects

* helloworldjvm: helloworld function running on the jvm
* helloworldjs: helloworld function running on node
* common: Common project code if you want to separate them out. This is a shared
  project base that can contain both jvm and js specific code.

## Output bundling

* dist: The output distribution director that consolidates all of the inputs
  either through copying or bundling.
  
You may run into issues with bundling the jvm project as creating a single
assembly means that you are combining other assemblies and there may be
conflicts e.g. the same file in the same "path" is included in 2 two different
jvm dependencies.

## Building
First install the javascript side of the application: 

```sh
npm install
```

## Deployment

## Development

You can run and compile the application several ways.

* `npm run start`: Start the app and open a web browser page.
* `npm run build`: Build the production app from npm. The output resides in dist.
* `sbt npmBuildFull`: Build the production app from sbt.
* `sbt npmBuildFast`: Build the fast app from sbt.

More commands...
* `npm run scala:full`: Performs `sbt fullOptJS` from npm.
* `npm run scala:fast`: Performs `sbt fastOptJS` from npm.
* `npm run scala:clean`: Performs `sbt clean` from npm.
* `sbt clean/fastOptJS/fullOptJS`: Runs sbt as you normally would.
* `npm run app`: Performs production webpack bundling.
* `npm run app:dev`: Performs dev webpack bundling.
* `npm run app:dev:start`: Stats webpack-dev-server using fast scala.

For development, since sbt watches the js and public assets directory, you can
use `sbt ~npmBuildFast` to watch all scala and non-scala assets. This runs the
scala build then calls webpack to create your output.

The "app" entries in package.json are used by sbt when sbt calls into the
javascript world to perform webpack bundling.

