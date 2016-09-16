
// For now, import everything from React-Bootstrap:
// This apparently imports everything:
//import { Button } from 'react-bootstrap';
//import ReactBootstrap from 'react-bootstrap';
// This imports the button only:
// import Button from 'react-bootstrap/lib/Button';
//import { Button } from 'react-bootstrap'; //  /es6/Button';
// import Button from 'react-bootstrap/es/Button';
import Button from '../node_modules/react-bootstrap/es/Button.js';
//import ButtonInput from '../node_modules/react-bootstrap/es/ButtonInput.js';
import Modal from '../node_modules/react-bootstrap/es/Modal.js';
import ModalHeader from '../node_modules/react-bootstrap/es/ModalHeader.js';
import ModalTitle from '../node_modules/react-bootstrap/es/ModalTitle.js';
import ModalBody from '../node_modules/react-bootstrap/es/ModalBody.js';
import ModalFooter from '../node_modules/react-bootstrap/es/ModalFooter.js';
import MenuItem from '../node_modules/react-bootstrap/es/MenuItem.js';

window['ReactBootstrap'] = {
  Button: Button,
  Modal: Modal,
  ModalHeader: ModalHeader,
  ModalTitle: ModalTitle,
  ModalBody: ModalBody,
  ModalFooter: ModalFooter,
  MenuItem: MenuItem,
};


