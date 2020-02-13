const path = require('path');
const webpack = require('webpack');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

// Get manifest
const normalizedPath = require('path').join(__dirname, './target/dependency');
let manifest = '';

require('fs').readdirSync(normalizedPath).forEach(function (file) {
    manifest = './target/dependency/' + file;
    console.log('Server Settings module uses manifest: ' + manifest);
});

module.exports = (env, argv) => {
    let config = {
        entry: {
            main: [path.resolve(__dirname, 'src/javascript/publicPath'), path.resolve(__dirname, 'src/javascript/index.js')]
        },
        output: {
            jsonpFunction: 'jahiaServerSettingsJsonp',
            path: path.resolve(__dirname, 'src/main/resources/javascript/apps/'),
            filename: 'jahia.bundle.js',
            chunkFilename: '[name].jahia.[chunkhash:6].js'
        },
        resolve: {
            mainFields: ['module', 'main'],
            extensions: ['.mjs', '.js', '.jsx', 'json']
        },
        optimization: {
            splitChunks: {
                maxSize: 400000
            }
        },
        module: {
            rules: [
                {
                    test: /\.jsx?$/,
                    include: [path.join(__dirname, 'src')],
                    loader: 'babel-loader',
                    query: {
                        presets: [
                            ['@babel/preset-env', {modules: false, targets: {safari: '7', ie: '10'}}],
                            '@babel/preset-react'
                        ],
                        plugins: [
                            '@babel/plugin-syntax-dynamic-import'
                        ]
                    }
                },
                {
                    test: /\.s[ac]ss$/i,
                    use: [
                        'style-loader',
                        {
                            loader:'css-loader',
                            options: {
                                modules: true
                            }
                        },
                        'sass-loader'
                    ]
                }
            ]
        },
        plugins: [
            new webpack.DllReferencePlugin({
                manifest: require(manifest)
            }),
            new CleanWebpackPlugin({verbose: false}),
            new webpack.HashedModuleIdsPlugin({
                hashFunction: 'sha256',
                hashDigest: 'hex',
                hashDigestLength: 20
            }),
            new CopyWebpackPlugin([{from: './package.json', to: ''}])
        ],
        mode: argv.mode
    };

    config.devtool = (argv.mode === 'production') ? 'source-map' : 'eval-source-map';

    if (argv.analyze) {
        config.devtool = 'source-map';
        config.plugins.push(new BundleAnalyzerPlugin());
    }

    return config;
};
