// for convenience in console debugging with eval
edu.wpi.cetask = Packages.edu.wpi.cetask;

// following must be at toplevel for invokeFunction (see TaskEngine.toString)

function edu_wpi_cetask_toString (obj) { return obj.toString(); }

function edu_wpi_cetask_newObject () { return new Object(); }

// default shallow cloning (as in Java)
function edu_wpi_cetask_clone (x) {
     if ( x == null || typeof x != "object" ) return x;
     // respect custom clone method if defined (to go deeper)
     if ( typeof x.clone == "function" ) return x.clone();
     if ( x instanceof java.lang.Object ) {
         if ( x instanceof java.lang.Cloneable ) return x.clone();
         throw new Error("Modified Java input object does not implement Cloneable: "+x);
     }
     // avoid calling constructor directly (may require arguments)
     var copy = Object.create(x.constructor.prototype);
     for (var p in x) {// works for arrays also
         // don't include inherited properties
         if (x.hasOwnProperty(p)) copy[p] = x[p];
     }
     return copy;           
  }


// for compatibility with Jint for Unity

Debug = { Log : function (obj) { println(obj); }}

// for Nashorn compatibility with Rhino

if ( (typeof println) === 'undefined' ) 
    println = function (obj) { print(obj+"\n"); }





