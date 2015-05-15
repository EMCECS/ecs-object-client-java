Command used to encrypt test data: 

openssl enc -in uncompressed.txt -out encrypted.txt.aes128 -aes-128-cbc -K 0d1ddf2e46d99cbfa37b8c186aa0a9b3 -iv A2643692065AB9690AE7CFE6F8489C86

Command used to add a key to the keystore:

keytool -genkeypair -keystore keystore.jks -alias masterkey -keyalg RSA -keysize 2048 -dname "CN=Storage Bot, OU=Advanced Software Division, O=EMC, L=Hopkinton, ST=MA, C=US"

