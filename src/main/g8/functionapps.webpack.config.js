const webpack = require("webpack")
const merge = require("webpack-merge")
const UglifyJsPlugin = require("uglifyjs-webpack-plugin")
const path = require("path")

// dest: output file in the dist function directory
function libraryOutput(dest) {
    return {
        output: {
            path: dest,
            // these are set to index.js which is what functionapp expects for js functions
            //filename: "[name].js",
            //library: "[name]",
            filename: "index.js",
            // assigns whatever is exported from entry point to module.exports
            libraryTarget: "commonjs2",
        }}
}

// fname: function name
// scalapath: relative path from topdir to scala output .js file
const common = (fname, scalapath) => ({
    // Entry point is set to the "shim" file called main.js. Each function needs one.
    entry: {
        [fname]: path.resolve(__dirname, fname + "/src/main/js/main.js")
    },
    target: "node",
    resolve: {
        symlinks: false,
        extensions: [".js", ".json", "*"],
        alias: {
            // all shim files should import scala which is set to the scala-js output
            scala: path.resolve(__dirname, scalapath),
        },
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                use: ["scalajs-friendly-source-map-loader"],
                enforce: "pre",
                exclude: [/node_modules/],
            },
        ]
    },
})

const dev = {
    devtool: "source-map",
    // we set mode = "none" elsewhere
    //mode: "development",
}

const prod = {
    devtool: "source-map",
    // we set mode = "none" elsewheree
    //mode: "production"
}

/**
 * env.name must be set to the function name.
 */
module.exports = function (env) {
    const isProd = (env && env.BUILD_KIND && env.BUILD_KIND==="production") || false
    console.log("isProd: ", isProd)    
    if(!env.name) throw new Error("env.name=<functionname> was not specified.")
    const fname = env.name
    console.log("function", fname)
    const scalapath = path.join(__dirname, fname + "/target/scala-2.12/" + fname + "-" + (isProd ? "opt.js":"fastopt.js"))
    // output goes to the dist/<function name> directory
    const output = libraryOutput(path.join(__dirname, "dist", fname))
    const globals = (nodeEnv) => ({
        "process.env": { "NODE_ENV": JSON.stringify(nodeEnv || "development") }
    })
    console.log("scalapath: ", scalapath)
    const modeNone = { mode: "none" }
    
    if (isProd) {
        const g = globals("production")
        console.log("Production build")
        console.log("globals: ", g)
        return merge(output, common(fname, scalapath), prod, modeNone, {
            plugins: [
                new webpack.DefinePlugin(g),
                new UglifyJsPlugin({
                    cache: true,
                    parallel: 4,
                    sourceMap: true,
                    uglifyOptions: { ecma: 5, compress: true }
                }),
            ]
        })
    }
    else {
        const g = globals()
        console.log("Dev build")
        console.log("globals: ", g)
        return merge(output, common(fname, scalapath), dev, modeNone, {
            plugins: [
                new webpack.HotModuleReplacementPlugin(),
                new webpack.DefinePlugin(g),
            ]
        })
    }
}
