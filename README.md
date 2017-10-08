# cie-linux-middleware

In order to compile the examples you need to install pcsclite.
On apt-based distributions, you can install it as
`sudo apt-get install pcsclite`

Three tests are included, they respectively allow:
+ reading NIS;
+ sending a verify message;
+ entering an interactive session to send custom APDU commands.

These esamples must be compiled with a C++11 compliant compiler.
