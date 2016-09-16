import babel from 'rollup-plugin-babel';
//import babelrc from 'babelrc-rollup';
import nodeResolve from 'rollup-plugin-node-resolve';

//let pkg = require('./package.json');
//let external = Object.keys(pkg.dependencies);

export default {
  entry: 'client/rollup/main-js.js',
  plugins: [
    //babel(babelrc()),
    babel(),
    nodeResolve({
			browser: true
		})
  ],
  targets: [
    {
      dest: 'satans-rollup-config-dest.js',
      format: 'umd',
      moduleName: 'rollupStarterProject',
      sourceMap: true
    }
  ]
};
