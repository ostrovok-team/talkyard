Static assets, i.e. images, fonts and transpiled scripts and styles, to be served
from https://server/-/assets/... .

In dev images, the public/ directory gets mounted here by docker-compose.yml.
(Built via docker-compose build web.)

In prod images, the public/ dir is copied to here and included in the image itself.
(Built via docker/build-web-prod.sh.)

