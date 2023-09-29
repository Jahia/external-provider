module.exports = {
    sourceMaps: 'inline',
    presets: [
        ['@babel/preset-env', {targets: {node: 'current'}}],
        '@babel/preset-typescript'
    ],
    plugins: [
        '@babel/plugin-transform-classes',
        '@babel/plugin-proposal-class-properties',
        ['@babel/plugin-transform-runtime', {regenerator: true}]
    ]
};
