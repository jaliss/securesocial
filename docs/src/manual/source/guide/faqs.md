---
file: faqs
---
# FAQs

### Why do I get a 401 when trying to authenticate against Twitter?
If you double checked your oauth keys and they're ok make sure the time in your computer is properly set. 

### How do I configure a callback url for Facebook? 
Facebook - and some other providers - won't redirect back to `localhost`. While in DEV mode you can do one of the following to use them:

- Use a public address
- Use a service dynamic dns service
- Add an alias in your `/etc/hosts` file that points to localhost

### Can I contribute to SecureSocial?
Yes, SecureSocial is an open source project and contributions are welcome. There are certain rules though:

- Before spending hours doing any work please let me know what you are planning so we can validate the approach is right and aligns well with SecureSocial.  Also, it could be that me or someone else is doing something similar and we can avoid duplicating efforts this way.
- Your code must be documented and if needed the User Guide must be updated as well or the contribution will not be accepted.
- **SecureSocial 2 is written in Scala and provides Java APIs too. This means you MUST code in Scala and add the necessary Java APIs if that functionality needs to be exposed to a developer using the module. If you send a contribution in Java only it will not be accepted.** Even if you insist :)

