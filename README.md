## IP access list for Play Framework 2.3

Simple IP access list for Play Framework Java controllers. Supports IPv4 IPs and CIDR.

Requirements:

- Play Framework 2.3.x
- Java 8

Dependencies:

    "commons-net" % "commons-net" % "3.3"

### Usage

- Copy [`RestrictToHostGroup.java`](https://github.com/franzgranlund/play2-restrict/blob/master/app/restrict/RestrictToHostGroup.java) and [`RestrictToHostGroupAction.java`](https://github.com/franzgranlund/play2-restrict/blob/master/app/restrict/RestrictToHostGroupAction.java) to a folder `app/restrict`.

- Add settings in application.conf:

        restricttohostgroup {
          groups {
            default = ["127.0.0.1", "192.168.7.58", "10.0.2.0/24"],
            intranet = ["10.0.1.96/28"]
          },
          redirect = "http://github.com"     # This is optional!
        }


- Configure your controller:

```java
@RestrictToHostGroup // Same as @RestrictToHostGroup("default")
public class Application extends Controller {

    public static Result index() {
        return ok("index - restricted to host group 'default'");
    }

    @RestrictToHostGroup("intranet")
    public static Result intranet() {
        return ok("intranet - restricted to host group 'intranet'");
    }
}
```
