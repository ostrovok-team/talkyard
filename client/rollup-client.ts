
// For now, import everything from React-Bootstrap:
// This apparently imports everything:
//import { Button } from 'react-bootstrap';
//import ReactBootstrap from 'react-bootstrap';
// This imports the button only:
// import Button from 'react-bootstrap/lib/Button';
//import Button from 'react-bootstrap/es6/Button';
import Button from '/home/kajmagnus/dev/test/rollup-starter-project/node_modules/react-bootstrap/es/Button.js';

//import React from 'react'; // zz

//console.log('Rollup dummy message ' + JSON.stringify(Button));
//console.log('boo, ' + JSON.stringify(React));

var ReactBootstrap = {
  Button: Button
};

window['ReactBootstrap'] = ReactBootstrap;
