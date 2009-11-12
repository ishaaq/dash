// Some predefined global functions we need.

// a temporary array containing all the functions we want the
// end user to be able to enumerate. After loading them the
// array will be removed from scope
enumerate = [ "load", "out", "print", "println", "typeOf" ];

function load() {
    var scriptFile = arguments[0];
    var args = arguments.length > 1 ? Array.prototype.slice.call(arguments, 1) : [];
    __shell__ .run(scriptFile, args);
}
load.help = "Usage: {{bold:load(scriptPath [,arg1] [,arg2]...):}}\n" +
"Loads the javascript file specified and additionally, if specified, the {{bold:arg:}}\n" +
"values will be used to construct an {{bold:arguments:}} array that the script can access.\n" +
"The {{bold:scriptPath:}} may be relative or absolute. If it cannot be located then a second\n" +
"attempt is made to locate it inside dash's {{bold:scripts:}} directory.\n";

// adapted from http://joncom.be/code/realtypeof/
function typeOf(v) {
    if(typeof(v) === "object") {
        if(v === null) return "null";
        if(v.constructor == (new Array).constructor) return "array";
        if(v.constructor == (new Date()).constructor) return "date";
        if(v instanceof java.lang.Object) return v.getClass().name;
        return "object";
    } else {
        return typeof(v);
    }
}
typeOf.help = "Usage: {{bold:typeOf(value):}}\n" +
"Displays the type of the {{bold:value:}} argument.  This implementation is a bit more complete\n" +
"than the in-built javascript {{bold:typeof:}} operator. In particular it differentiates between\n" +
"object types.\n";

function print(str, newline) {
    if (str === undefined) {
        str = "undefined";
    } else {
        if (str === null) {
            str = "null";
        }
    }
    out.print(String(str));
    if (newline) {
        out.print("\n");
    }
    out.flush();
}
print.help = "Usage: {{bold:print(string, boolean):}}\n" +
"Prints the {{bold:string:}} and a newline char if the {{bold:boolean:}} is true. The printing is done on\n" +
"the client console using dash's {{bold:RemoteWriter:}} implementation.\n";

function println(str) {
    print(str, true);
}
println.help = "Usage: {{bold:println(string):}}\n" +
"Prints the {{bold:string:}} and a newline char. The printing is done on\n" +
"the client console using dash's {{bold:RemoteWriter:}} implementation.\n";

function __desc__() {
    var describe = function(name, verbose) {
        try {
            var obj = eval(name);
            println("{{bold:" + name + ":}}\t: " + typeOf(obj));
            if(verbose) {
                if(typeof obj.help == "string") {
                    println(obj.help);
                } else if(typeof obj.help === "function") {
                    var help = obj.help();
                    if(typeOf(help) === "java.lang.String" || typeOf(help) === "string") {
                        println(help);
                    }
                }
                if(typeOf(obj) === "object") {
                    println("--------------------------------------------------------");
                    var propNames = [];
                    for(var i in obj) {
                        // no point describing the help string if it exists, we've already
                        // printed it above
                        if(typeof i === "string" && (i !== "help" || typeof obj[i] !== 'string')) {
                            propNames.push(i);
                        }
                    }
                    for(var i in propNames.sort()) {
                        describe(name + "." + propNames[i], false);
                    }
                }
            }
        } catch (e) {
            println("{{red:ERR: :}}'" + name + "' is not a valid identifier.");
        }
    };

    if(arguments.length === 1) {
        // we're describing a single object, we can be more descriptive:
        describe(arguments[0], true);
    } else {
        for(var i = 0; i < arguments.length; i++) {
             describe(arguments[i], false);
        }
    }
}

