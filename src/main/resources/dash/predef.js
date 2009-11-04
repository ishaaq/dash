// Some predefined global functions we need.

// a temporary array containing all the functions we want the
// end user to be able to enumerate. After loading them the
// array will be removed from scope
enumerate = [ "load", "out", "print", "println", "typeOf" ]

load = function() {
     var scriptFile = arguments[0]
    var args = arguments.length > 1 ? Array.prototype.slice.call(arguments, 1) : []
    __shell__ .run(scriptFile, args)
}

load.help = "Usage: load(scriptPath [,arg1] [,arg2]...)\n" +
"Loads the javascript file specified and additionally, if specified, the arg\n" +
"values will be used to construct an 'arguments' array that the script can access.\n" +
"The scriptPath may be relative or absolute. If it cannot be located then a second\n" +
"attempt is made to locate it inside dash's scripts directory."

// adapted from http://joncom.be/code/realtypeof/
function typeOf(v) {
    if(typeof(v) == "object") {
        if(v === null) return "null"
        if(v.constructor == (new Array).constructor) return "array"
        if(v.constructor == (new Date).constructor) return "date"
        if(v instanceof java.lang.Object) return v.getClass().name
        return "object"
    } else {
        return typeof(v)
    }
}

typeOf.help = "Usage: typeOf(value)\n" +
"Displays the type of the value specified.  This implementation is a bit more complete\n" +
"than the in-built javascript 'typeof' operator. In particular it differentiates between\n" +
"object types."

__desc__ = function() {
    var describe = function(name, verbose) {
        try {
            var obj = eval(name)
            println(name + "\t: " + typeOf(obj))
            if(verbose) {
                if(typeof obj.help == "string") {
                    println(obj.help)
                } else if(typeof obj.help == "function") {
                    var help = obj.help()
                    if(typeOf(help) == "java.lang.String" || typeOf(help) == "string") {
                        println(help)
                    }
                }
                if(typeOf(obj) == "object") {
                    for(i in obj) {
                        // no point describing the help string if it exists, we've already
                        // printed it above
                        if(i != "help" || typeof obj[i] != 'string') {
                            describe(name + "." + i, false)
                        }
                    }
                }
            }
        } catch (e) {
            println("[ERR] " + name + " is not a valid identifier.")
        }
    }

    if(arguments.length == 1) {
        // we're describing a single object, we can be more descriptive:
        describe(arguments[0], true)
    } else {
        for(var i = 0; i < arguments.length; i++) {
             describe(arguments[i], false)
        }
    }
}

function print(str, newline) {
    if (typeof (str) == "undefined") {
        str = "undefined";
    } else {
        if (str == null) {
            str = "null";
        }
    }
    out.print(String(str));
    if (newline) {
        out.print("\n");
    }
    out.flush();
}
print.help = "Usage: print(string, boolean)\n" +
"Prints the string and a newline char if the boolean is true. The printing is done on\n" +
"the client side using dash's RemoteWriter implementation.\n"

function println(str) {
    print(str, true);
}
println.help = "Usage: println(string)\n" +
"Prints the string and a newline char. The printing is done on\n" +
"the client side using dash's RemoteWriter implementation.\n"
