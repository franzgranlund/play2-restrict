package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import restrict.RestrictToHostGroup;

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
