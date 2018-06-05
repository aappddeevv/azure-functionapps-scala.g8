/** 
 * Each function should have a shim since azure functionapps need module.exports
 * set to a function, which is not possible from scala-js directly.
 * The import "scala" is aliased in the webpack config file to the correct
 * scala-js compiler output. You can pull some other tricks using
 * webpack loaders, but this is straight forward.
 */
const x = require("scala")
module.exports = x.main
