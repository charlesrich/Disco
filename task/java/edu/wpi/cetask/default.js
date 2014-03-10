var edu = {};
edu.wpi = {};

var $plan;

// for convenience in console debugging with eval
edu.wpi.cetask = Packages.edu.wpi.cetask;

// following must be at toplevel for invokeFunction (see TaskEngine.toString)

function edu_wpi_cetask_toString (obj) { return obj.toString(); }

function edu_wpi_cetask_newObject () { return new Object(); }

// **NB** If adding functions here, compile them in TaskEngine constructor!

edu.wpi.cetask_helper = {

  // helper functions for defining Java equals and hashCode for edu.wpi.cetask.Task

  equals:
    function (x1, x2) {
       for (var name in x1) { 
          var value = x1[name]; 
          if ( !(value instanceof java.lang.Object ? 
                 value.equals(x2[name]) : value == x2[name]) ) 
             return false;
       }       
       return true;
  },
  
  hashCode:  
    function hashCode (x) { // name required for recursive definition below
       var hash = 0;
       var factor = 1;
       switch (typeof x) {
           case 'number': return x;
           case 'boolean': return x+1;
           case 'string': 
             for (i = x.length; i-- > 0;) {
                factor *= 31;
                hash += x.charCodeAt(i)*factor;
             }
             return hash;
           default:
             if ( x instanceof java.lang.Object ) return x.hashCode();
             for (var p in x) {
                factor *= 31;
                var value = x[p];
                // ignore non-Java nested objects to avoid infinite recursion
                if ( typeof value != 'object' || value instanceof java.lang.Object )
                   hash += hashCode(value)*factor;
             }
             return hash;
         } 
    },

  clone:
   function (x) { // default shallow cloning (as in Java)
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
}

// for compatibility with Jint for Unity

Debug = { Log : function (obj) { println(obj); }}
