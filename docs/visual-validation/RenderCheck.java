import java.lang.reflect.*;
import java.io.*;
import java.nio.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import org.lwjgl.*;
import org.lwjgl.opengl.*;
import dev.fluxarc.structure.StructureSpec;

/** Isolated real OpenGL mesh check, deliberately does not launch Minecraft. */
public final class RenderCheck {
 static final int W=640,H=480;
 public static void main(String[] args) throws Exception {
  Pbuffer context=new Pbuffer(W,H,new PixelFormat(8,24,0),null,null);
  context.makeCurrent();
  System.out.println("GL_RENDERER="+GL11.glGetString(GL11.GL_RENDERER));
  System.out.println("GL_VERSION="+GL11.glGetString(GL11.GL_VERSION));
  Class<?> renderer=Class.forName("dev.fluxarc.client.ArcRenderer");
  Method cube=renderer.getDeclaredMethod("cube",float.class,float.class,float.class,float.class,float.class,float.class,float[].class);cube.setAccessible(true);
  String[] names={"forge","swarm","fold","recycle","probe","tactical"};
  float[][] colors={{.13f,.18f,.22f},{.77f,.82f,.83f},{.08f,.81f,.98f},{.2f,.25f,.3f},{.42f,.5f,.56f}};
  float[] phases={.12f,.38f,.65f,.94f};
  BufferedImage sheet=new BufferedImage(W*4,H*6,BufferedImage.TYPE_INT_RGB);
  Graphics2D graphics=sheet.createGraphics();
  for(int type=0;type<6;type++) for(int phase=0;phase<4;phase++) {
   GL11.glViewport(0,0,W,H);GL11.glClearColor(.025f,.035f,.055f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);
   GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(-6.3,6.3,-4.725,4.725,-40,40);
   GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glRotatef(23,1,0,0);GL11.glRotatef(-37,0,1,0);GL11.glTranslatef(0,-2,0);
   cube.invoke(null,0f,-.65f,0f,5.2f,.08f,5.2f,new float[]{.065f,.08f,.1f});
   cube.invoke(null,0f,0f,0f,.5f,.5f,.5f,colors[0]);
   for(StructureSpec.Part p:StructureSpec.parts(type))if(p.meta>=0) {
    // Match ArcBlock.setBlockBoundsBasedOnState exactly; use flat proxy colors, not texture sampling.
    float halfXZ=p.meta==0?.3f:p.meta==1||p.meta==4?.5f:.35f;
    float halfY=p.meta==4?.15f:.5f;
    cube.invoke(null,(float)p.x,(float)p.y,(float)p.z,halfXZ,halfY,halfXZ,colors[p.meta]);
   }
   Method action;
   if(type==0) {action=renderer.getDeclaredMethod(names[type],float.class,float.class,boolean.class,boolean.class);action.setAccessible(true);action.invoke(null,phases[phase]*12,phases[phase],true,false);}
   else if(type==2){action=renderer.getDeclaredMethod(names[type],float.class,float.class,boolean.class,int.class);action.setAccessible(true);action.invoke(null,phases[phase]*12,phases[phase],true,phase);}
   else {action=renderer.getDeclaredMethod(names[type],float.class,float.class,boolean.class);action.setAccessible(true);action.invoke(null,phases[phase]*12,phases[phase],true);}
   GL11.glFinish();int error=GL11.glGetError();if(error!=GL11.GL_NO_ERROR)throw new AssertionError("OpenGL error "+error+" "+names[type]);
   ByteBuffer pixels=BufferUtils.createByteBuffer(W*H*4);GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
   BufferedImage frame=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);int lit=0;
   for(int yy=0;yy<H;yy++)for(int xx=0;xx<W;xx++){int offset=(yy*W+xx)*4;int r=pixels.get(offset)&255,g=pixels.get(offset+1)&255,b=pixels.get(offset+2)&255;frame.setRGB(xx,H-1-yy,(r<<16)|(g<<8)|b);if(r+g+b>100)lit++;}
   if(lit<1000)throw new AssertionError("Unexpected dark frame "+lit);
   Graphics2D label=frame.createGraphics();label.setColor(Color.WHITE);label.setFont(new Font("SansSerif",Font.PLAIN,18));label.drawString(names[type]+" / progress "+phases[phase],18,28);label.setFont(new Font("SansSerif",Font.PLAIN,12));label.drawString("OpenGL mesh verification - not Minecraft gameplay",18,H-18);label.dispose();
   ImageIO.write(frame,"png",new File(args[0],names[type]+"-"+phase+".png"));graphics.drawImage(frame,phase*W,type*H,null);
   System.out.println("PASS "+names[type]+" p="+phases[phase]+" litPixels="+lit+" glError=0");
  }
  graphics.dispose();ImageIO.write(sheet,"jpg",new File(args[0],"mesh-contact-sheet.jpg"));context.destroy();
 }
}
