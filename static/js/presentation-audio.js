(function(){
'use strict';
class Player {
 constructor(){this.audio=null;this.cancel=null;this.userPaused=false;}
 has(text){return !!window.ZIPAI_AUDIO_MANIFEST?.[text];}
 stop(){if(this.cancel)this.cancel();if(this.audio){this.audio.pause();this.audio.removeAttribute('src');this.audio.load();}this.audio=null;this.cancel=null;this.userPaused=false;}
 pause(){this.userPaused=true;this.audio?.pause();}
 resume(){this.userPaused=false;if(this.audio)this.audio.play().catch(()=>this.cancel?.('error'));}
 play(text,rate){this.stop();const path=window.ZIPAI_AUDIO_MANIFEST?.[text];if(!path)return Promise.resolve('missing');
  return new Promise(resolve=>{const a=new Audio(path);this.audio=a;a.playbackRate=Math.max(0.5,Math.min(2,Number(rate)||1));
   let done=false,elapsed=0;const finish=result=>{if(done)return;done=true;clearInterval(timer);a.onended=a.onerror=null;resolve(result);};
   this.cancel=(result='cancelled')=>finish(result);
   const timer=setInterval(()=>{if(!this.userPaused)elapsed+=1000;if(elapsed>Math.max(60000,text.length*250))finish('error');},1000);
   a.onended=()=>finish('ended');a.onerror=()=>finish('error');a.play().catch(()=>finish('error'));
  });
 }
}
window.ZipaiAudioPlayer=Player;
const prototype=window.ZipaiPresentation.Narrator.prototype;
const originalSpeak=prototype.speak,originalStop=prototype.stop,originalPause=prototype.togglePause;
prototype.stop=function(){this.recorded?.stop();return originalStop.call(this);};
prototype.speak=function(text,voice,rate){
 this.recorded??=new Player();
 if(!this.recorded.has(text))return originalSpeak.call(this,text,voice,rate);
 this.stop();const token=this.token;this.active=true;this.onState('speaking');
 this.recorded.play(text,rate).then(result=>{
  if(token!==this.token)return;
  if(result==='ended'){this.active=false;this.paused=false;this.onState('finished');this.onEnd();}
  else if(result==='error')originalSpeak.call(this,text,voice,rate);
 });return true;
};
prototype.togglePause=function(){if(!this.recorded?.audio)return originalPause.call(this);
 if(!this.active)return;this.paused=!this.paused;
 if(this.paused)this.recorded.pause();else this.recorded.resume();this.onState(this.paused?'paused':'speaking');
};
})();
