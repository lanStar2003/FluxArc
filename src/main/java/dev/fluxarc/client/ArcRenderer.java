package dev.fluxarc.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dev.fluxarc.tile.TileMachine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/** Procedural solid meshes. All animation is driven by synchronized server state. */
@SideOnly(Side.CLIENT)
public final class ArcRenderer extends TileEntitySpecialRenderer {
    private static final float[] DARK={.13f,.18f,.22f}, WHITE={.77f,.82f,.83f}, CYAN={.08f,.81f,.98f}, HOT={1f,.31f,.055f};
    @Override public void renderTileEntityAt(TileEntity raw,double x,double y,double z,float partial) {
        if(!(raw instanceof TileMachine)) return;
        TileMachine tile=(TileMachine)raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LINE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glTranslated(x+.5,y+.5,z+.5);
            GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);
            boolean running=tile.isRunning();
            float p=tile.state==4?1:Math.max(0,Math.min(1,tile.animationProgress()));
            float motion=tile.progress/20f;
            if(running&&tile.machineType()==4) {float[] limits={0,.25f,.5f,.85f,1};int s=Math.max(0,Math.min(3,tile.stage));p=limits[s]+tile.phaseProgress()*(limits[s+1]-limits[s]);}
            float[] status=tile.state==5?new float[]{.85f,.14f,.13f}:tile.state==3||tile.state==6||tile.state==7?HOT:CYAN;
            // Narrow controller status bezel, with a different pulse for stopped machines.
            ring(0,.52f,0,.43f,.055f,.045f,0,status,12);
            if(!tile.formed) return;
            switch(tile.machineType()) {
                case 0:forge(motion,p,running,tile.state==4);break;
                case 1:swarm(motion,p,running);break;
                case 2:fold(motion,p,running,tile.stage);break;
                case 3:recycle(motion,p,running);break;
                case 4:probe(motion,p,running);break;
                case 5:tactical(motion,p,running);break;
                default:break;
            }
        } finally { GL11.glPopAttrib();GL11.glPopMatrix(); }
    }
    private static void forge(float t,float p,boolean on,boolean done) {
        for(int sx:new int[]{-1,1}) for(int sz:new int[]{-1,1}) {
            beam(sx*3,1,sz*3,sx*2,3,sz*2,.17f,DARK);
            float swing=on?(float)Math.sin(Math.min(1,p*8)*Math.PI/2)*.38f:0;
            beam(sx*2,3,sz*2,sx*(1.35f-swing),3.3f,sz*(1.35f-swing),.13f,WHITE);
            cube(sx*(1.35f-swing),3.3f,sz*(1.35f-swing),.24f,.1f,.24f,CYAN);
        }
        float squeeze=on&&p>.5f&&p<.8f?(float)Math.sin((p-.5f)/.3f*Math.PI)*.6f:0;
        ring(0,3.6f,0,1.7f-squeeze,.18f,.2f,t*17,DARK,12);
        ring(0,3.42f,0,1.7f-squeeze,.045f,.08f,t*17,CYAN,12);
        if(!on&&!done) return;
        if(p<.23f&&!done) {
            float lift=Math.min(1,p/.18f);
            for(int k=0;k<3;k++) cube((k-1)*.55f,.8f+lift*1.6f,0,.2f,.19f,.2f,WHITE);
        } else if(p<.56f&&!done) {
            float merge=Math.min(1,(p-.23f)/.33f);
            for(int k=0;k<5;k++) {
                double a=k*Math.PI*2/5+t*.7;
                droplet((float)Math.cos(a)*(1-merge),(float)(2.5+Math.sin(a*2)*.1),(float)Math.sin(a)*(1-merge),.2f+merge*.03f,HOT);
            }
        } else {
            float fall=done?1:Math.max(0,(p-.86f)/.14f);
            float cool=Math.max(0,Math.min(1,(p-.68f)/.2f));
            gear(0,2.5f-fall*1.65f,0,.62f,t*12*(1-fall),mix(HOT,WHITE,cool));
        }
        if(on) for(int k=0;k<4;k++) {double a=k*Math.PI/2+t;beam((float)Math.cos(a)*1.45f,3.4f,(float)Math.sin(a)*1.45f,0,2.5f,0,.014f,CYAN);}
    }
    private static void swarm(float t,float p,boolean on) {
        for(int s:new int[]{-1,1}) {
            float reach=on?.65f+(float)Math.sin(t*1.8+s)*.25f:1.15f;
            beam(s*2,3,0,s*reach,2.6f,(float)Math.sin(t)*.6f,.15f,WHITE);
            beam(s*reach,2.6f,(float)Math.sin(t)*.6f,s*.4f,1.4f+(on?p:0),0,.075f,DARK);
            cube(s*.4f,1.4f+(on?p:0),0,.1f,.08f,.1f,CYAN);
        }
        wireBox(0,1.65f,0,.75f,.7f,.65f,CYAN);
        if(on) {
            for(int i=0;i<8;i++) if(p>=i/8f) cube(0,1.02f+i*.16f,0,.54f,.065f,.42f,i%2==0?DARK:WHITE);
            for(int k=0;k<12;k++) {double a=t*2+k*2.399;cube((float)Math.cos(a)*.85f,1.2f+(k%4)*.32f,(float)Math.sin(a)*.85f,.035f,.035f,.035f,CYAN);}
        }
    }
    private static void fold(float t,float p,boolean on,int stage) {
        for(int k=0;k<3;k++) {
            GL11.glPushMatrix();GL11.glTranslatef(0,2.5f,0);GL11.glRotatef(k*60+t*(k%2==0?15:-19),0,1,0);GL11.glRotatef(65+k*12,1,0,0);
            ring(0,0,0,1.1f+k*.3f,.085f,.09f,0,k==Math.abs(stage)%3?CYAN:WHITE,16);GL11.glPopMatrix();
        }
        if(on) { GL11.glPushMatrix();GL11.glTranslatef(0,2.5f,0);GL11.glRotatef(t*27,1,1,0);cube(0,0,0,.3f,.3f,.3f,stage%2==0?HOT:CYAN);GL11.glPopMatrix(); }
        for(int k=0;k<6;k++) cube((k-2.5f)*.19f,.7f,1.3f,.065f,.065f,.065f,k<=stage?CYAN:DARK);
    }
    private static void recycle(float t,float p,boolean on) {
        float scan=on?1+(float)(.5+.5*Math.sin(t*2))*2:3.4f;
        beam(-1.8f,scan,-1.8f,1.8f,scan,-1.8f,.045f,CYAN);
        if(on) {
            for(int k=0;k<8;k++) {
                float spread=p>.35f?(p-.35f)*2:0;
                float xx=((k&1)==0?-1:1)*(.2f+spread),zz=((k&2)==0?-1:1)*(.2f+spread);
                float yy=2+((k&4)==0?-.22f:.22f)-Math.max(0,p-.75f)*3;
                cube(xx,yy,zz,.17f*(1-p*.6f),.17f,.17f,k%3==0?HOT:WHITE);
                if(p<.65f) beam(-1.6f,scan,-1.7f,xx,yy,zz,.012f,CYAN);
            }
        }
    }
    private static void probe(float t,float p,boolean on) {
        beam(-1,1,-1,-1,4,-4,.12f,DARK);beam(1,1,-1,1,4,-4,.12f,DARK);
        for(int i=1;i<=4;i++) cube(0,i-.1f,-i,.82f,.08f,.1f,on&&p*6>i?CYAN:DARK);
        float distance=0;
        if(on&&p>.25f&&p<.5f) distance=(p-.25f)*18;
        else if(on&&p>=.5f&&p<.85f) return;
        else if(on&&p>=.85f) distance=(1-p)*30;
        GL11.glPushMatrix();GL11.glTranslatef(0,1.4f+distance,-1.4f-distance);GL11.glRotatef(-45,1,0,0);
        cube(0,0,0,.21f,.65f,.21f,WHITE);cube(0,.68f,0,.14f,.16f,.14f,DARK);
        cube(-.38f,-.28f,0,.2f,.32f,.06f,DARK);cube(.38f,-.28f,0,.2f,.32f,.06f,DARK);
        if(on&&distance>0) {cube(0,-.8f,0,.14f,.14f,.14f,CYAN);beam(0,-.9f,0,0,-1.5f,0,.04f,HOT);}
        GL11.glPopMatrix();
    }
    private static void tactical(float t,float p,boolean on) {
        for(int sx:new int[]{-1,1}) for(int sz:new int[]{-1,1}) {
            cube(sx*4,2.55f,sz*4,.16f,.12f,.16f,CYAN);
            if(on) beam(sx*4,2.5f,sz*4,sx*3,2.1f,sz*3,.02f,CYAN);
        }
        if(on) {
            float r=.2f+(t%3)/3*3.5f;
            ring(0,.56f,0,r,.025f,.025f,0,CYAN,48);
            float scan=.8f+(float)(.5+.5*Math.sin(t))*2;
            for(int i=-3;i<=3;i++) beam(i,scan,-3,i,scan,3,.007f,CYAN);
        }
    }
    private static float[] mix(float[] a,float[] b,float t){return new float[]{a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,a[2]+(b[2]-a[2])*t};}
    private static void droplet(float x,float y,float z,float size,float[] c) {
        GL11.glPushMatrix();GL11.glTranslatef(x,y,z);GL11.glRotatef(45,0,1,0);cube(0,0,0,size*.8f,size,size*.8f,c);GL11.glRotatef(45,0,1,0);cube(0,0,0,size*.65f,size*.85f,size*.65f,c);GL11.glPopMatrix();
    }
    private static void gear(float x,float y,float z,float r,float rot,float[] c) {
        ring(x,y,z,r*.66f,.16f,.12f,rot,c,16);
        for(int k=0;k<12;k++){double a=(rot+k*30)*Math.PI/180;GL11.glPushMatrix();GL11.glTranslatef(x+(float)Math.cos(a)*r,y,z+(float)Math.sin(a)*r);GL11.glRotatef((float)-Math.toDegrees(a),0,1,0);cube(0,0,0,.16f,.12f,.105f,c);GL11.glPopMatrix();}
    }
    private static void ring(float x,float y,float z,float r,float thick,float height,float rot,float[] c,int count) {
        for(int k=0;k<count;k++) {float angle=rot+k*360f/count;double a=angle*Math.PI/180;GL11.glPushMatrix();GL11.glTranslatef(x+(float)Math.cos(a)*r,y,z+(float)Math.sin(a)*r);GL11.glRotatef(-angle,0,1,0);cube(0,0,0,thick,height,(float)Math.PI*r/count*.84f,c);GL11.glPopMatrix();}
    }
    private static void wireBox(float x,float y,float z,float a,float b,float c,float[] color) {
        for(int s:new int[]{-1,1}) for(int q:new int[]{-1,1}) {beam(x-a,y+s*b,z+q*c,x+a,y+s*b,z+q*c,.013f,color);beam(x+s*a,y-b,z+q*c,x+s*a,y+b,z+q*c,.013f,color);beam(x+s*a,y+q*b,z-c,x+s*a,y+q*b,z+c,.013f,color);}
    }
    private static void beam(float x1,float y1,float z1,float x2,float y2,float z2,float radius,float[] c) {
        float dx=x2-x1,dy=y2-y1,dz=z2-z1,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<.0001f)return;
        GL11.glPushMatrix();GL11.glTranslatef((x1+x2)/2,(y1+y2)/2,(z1+z2)/2);
        if(Math.abs(dy/len)<.9999)GL11.glRotatef((float)Math.toDegrees(Math.acos(dy/len)),dz,0,-dx);
        cube(0,0,0,radius,len/2,radius,c);GL11.glPopMatrix();
    }
    private static void cube(float x,float y,float z,float a,float b,float c,float[] color) {
        double[][] v={{x-a,y-b,z-c},{x+a,y-b,z-c},{x+a,y+b,z-c},{x-a,y+b,z-c},{x-a,y-b,z+c},{x+a,y-b,z+c},{x+a,y+b,z+c},{x-a,y+b,z+c}};
        int[][] faces={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        float[] shade={.68f,.87f,.62f,.8f,1f,.45f};
        Tessellator tess=Tessellator.instance;tess.startDrawingQuads();
        for(int f=0;f<6;f++){tess.setColorOpaque_F(color[0]*shade[f],color[1]*shade[f],color[2]*shade[f]);for(int i:faces[f])tess.addVertex(v[i][0],v[i][1],v[i][2]);}tess.draw();
    }
}
