import { Actor, HttpAgent } from '@dfinity/agent';
import { AuthClient } from "@dfinity/auth-client";
import { Principal } from "@dfinity/agent"; 

/********************************* */
// Canister Id as an example
const androidCanisterId = 'im7ut-faaaa-aaaah-qc5ra-cai';

var rareIndex = 0;
 
/***********************************************/

const connectionState = false;


const agent = new HttpAgent();
let principal = new Principal();


var seconds = 0;



const whitelist = [androidCanisterId];


const ICPStepFactory =  ({ IDL }) => {
  return IDL.Service({
    'getImage' : IDL.Func([IDL.Nat], [IDL.Text], ['query']),
    'getImageSize' : IDL.Func([], [IDL.Nat], []),
  });
};

var el = document.getElementById('seconds-counter');


function start() {
  var tick = function(){
    // console.log("Ticket " + seconds);
    seconds += 1;
    el.innerText = "Minting Time " + seconds + " seconds.";
  };

  var timerId = setTimeout(tick, 1000);
  return timerId
};

function stop(timerId) {
  clearTimeout(timerId);
};


var stopFlag = false;
var running = false;
var timerId = 0;

function stopBar()
{
	stopFlag  = true;
  running   = false;
  //stop(timerId);
}

function startBar() {

  //timerId = start();
  var i = 0;
  stopFlag = false;
  running = true;
  if (i == 0) {
    i = 1;
    var elem = document.getElementById("myBar");
    var width = 1;
    var id = setInterval(frame, 50);
    function frame() {
      if(stopFlag)
      {
        running = false;
      	clearInterval(id);
        width = 100;
        elem.style.width = width + "%";
      }
      if (width >= 100) {
        width = 0;
        i = 0;
      } else {
        width++;
        elem.style.width = width + "%";
      }
    }
  }
}
/****************************************** */
async function init()
{
  const authClient = await AuthClient.create();
  if (await authClient.isAuthenticated()) {
    handleAuthenticated(authClient);
  };
};


 
(async () => {
  const result = await window?.ic?.plug?.requestConnect({
    whitelist,
    host:"https://mainnet.dfinity.network",
  }); 
  const connectionState = result ? true : false;
  console.log(`The Connection Result ${result}!`);
  console.log(`The Connection was ${connectionState}!`);
})();


document.getElementById("getImage").addEventListener("click", async () => {

  startBar();
  getMyTokens();
  
});

async function  getMyTokens()
{

  //Android 
   
  // Create an actor to interact with the NNS Canister
  // we pass the NNS Canister id and the interface factory
  const androidActor = await window.ic.plug.createActor({
    canisterId: androidCanisterId,
    interfaceFactory: ICPStepFactory,
  });

   // const principalId = await window.ic.plug.agent.getPrincipal();

    var imageID = document.getElementById("androidimageid").value;

    var intImage = parseInt(imageID);

    console.log("Image ID = " + imageID);
    console.log("Integer ID");
    console.log(intImage);

    //console.log(principalId);

    androidimageid
    
    let imageString  = await androidActor.getImage(intImage);  
    let imageString2 = imageString.replace(/\n/g,'');

    imageString2 = "data:image/png;base64," + imageString2;

    document.getElementById("bunny").src= imageString2;	

    stopBar();


}

 
