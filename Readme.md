Unified Java Object Client SDK for the ECS Platform
===

[Javadocs can be found here](http://emcecs.github.io/ecs-object-client-java/latest/javadoc/)

Current APIs:

- S3

Targeted for Java 1.6+

**Currently, read timeout is disabled to avoid conflicts in updating.**

Here is an example for a conflict occurred with only one user when read timeout is enabled:
1. the client sends a empty put request 1.a to ECS
2. \>60s with no response for 1.a, thus, the client send a retry request 1.b
3. 1.b get 200 response
4. then the client update the object with request 2, also get 200 response
5. At this point request 1.a actually succeeded on ECS, the update 2 was overwrite to empty.

You can avoid this scenario by using IfNoneMatch and IfMatch as an optimistic lock:

Usage:

In #1, add headers to the request`request.withIfMatch(null).withIfNoneMatch("*");` this will only create the object when it does not exist

Similarly, when you want to sequentially update an object, you can use `request.withIfUnmodifiedSince(null).withIfMatch(lastEtag);` to ensure it just follows the previous update

How to enable read timeout: `s3Config.setReadTimeout(milliseconds)`
