// for convenience in console debugging with eval
edu.wpi.cetask = Packages.edu.wpi.cetask;

// following must be at toplevel for invokeFunction (see TaskEngine.toString)

function edu_wpi_cetask_toString (obj) { return obj.toString(); }

// default shallow cloning (as in Java)
function edu_wpi_cetask_clone (x) {
     if ( x == null || typeof x != "object" ) return x;
     // will use custom clone method if defined in JavaScript (to go deeper)
     if ( typeof x.clone == "function" ) {
       try { return x.clone(); }
       catch (e) { throw new Error(e+" calling clone method on modified input value: "+x) }
     }
     if ( x.constructor != undefined ) {
       // avoid calling constructor directly (may require arguments)
       var copy = Object.create(x.constructor.prototype);
       for (var p in x) {// works for arrays also
         // don't include inherited properties
         if (x.hasOwnProperty(p)) copy[p] = x[p];
       }
       return copy;
     } else throw new Error("No clone method for modified input value: "+x);            
  }

// for compatibility with Jint for Unity

Debug = { Log : function (obj) { println(obj); }}

// for Nashorn compatibility with Rhino

if ( (typeof println) === 'undefined' ) 
    println = function (obj) { print(obj+"\n"); }





