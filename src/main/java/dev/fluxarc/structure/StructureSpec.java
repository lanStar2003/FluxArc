package dev.fluxarc.structure;

import dev.fluxarc.FluxArc;
import dev.fluxarc.tile.TileMachine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** World aligned structures: controller = (0,0,0), north = negative Z. */
public final class StructureSpec {
    public static final class Part {
        public final int x, y, z, meta;
        public Part(int x, int y, int z, int meta) { this.x=x; this.y=y; this.z=z; this.meta=meta; }
    }
    private static final List<List<Part>> ALL = new ArrayList<List<Part>>();
    static {
        for (int type=0;type<6;type++) {
            List<Part> p=new ArrayList<Part>();
            // Sparse cross-shaped mounting platform, never a closed cuboid shell.
            for(int i=-2;i<=2;i++) if(i!=0) { add(p,i,0,0,1); add(p,0,0,i,1); }
            if(type==0) {
                for(int sx:new int[]{-1,1}) for(int sz:new int[]{-1,1}) {
                    add(p,3*sx,0,3*sz,0);add(p,3*sx,1,3*sz,0);
                    add(p,3*sx,2,2*sz,1);add(p,2*sx,3,2*sz,2);
                }
                ring(p,4,2,4);
            } else if(type==1) {
                for(int sx:new int[]{-1,1}) {
                    add(p,sx*3,0,0,0);add(p,sx*3,1,0,1);add(p,sx*3,2,0,2);
                    add(p,sx*2,3,0,0);add(p,sx,3,0,2);
                }
                add(p,0,1,-3,0);add(p,0,2,-3,1);add(p,0,3,-3,2);
            } else if(type==2) {
                ring(p,1,3,0);ring(p,4,2,4);
                for(int sx:new int[]{-1,1}) {add(p,sx*3,2,0,2);add(p,sx*3,3,0,1);}
            } else if(type==3) {
                for(int x=-2;x<=2;x++) { add(p,x,1,-2,0);add(p,x,4,-2,2); }
                for(int y=2;y<=3;y++) {add(p,-2,y,-2,1);add(p,2,y,-2,1);}
                add(p,-3,1,1,2);add(p,3,1,1,2);
            } else if(type==4) {
                for(int i=1;i<=4;i++) {add(p,-1,i,-i,3);add(p,1,i,-i,3);}
                add(p,-2,4,-4,4);add(p,2,4,-4,4);add(p,0,5,-4,4);
                add(p,-3,1,1,2);add(p,3,1,1,2);add(p,0,1,3,1);
            } else {
                for(int sx:new int[]{-1,1}) for(int sz:new int[]{-1,1}) {
                    add(p,sx*4,0,sz*4,0);add(p,sx*4,1,sz*4,1);add(p,sx*4,2,sz*4,2);
                }
                for(int i=-3;i<=3;i++) {add(p,i,0,-4,0);add(p,i,0,4,0);add(p,-4,0,i,0);add(p,4,0,i,0);}
            }
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) for(int y=1;y<=2;y++) {
                // The launch rails enter this volume; their two occupied cells are intentional.
                if(type==4&&y==1&&z==-1&&Math.abs(x)==1) continue;
                add(p,x,y,z,-1);
            }
            ALL.add(Collections.unmodifiableList(p));
        }
    }
    private static void ring(List<Part> p,int y,int r,int meta) {
        for(int i=-1;i<=1;i++) {add(p,i,y,-r,meta);add(p,i,y,r,meta);add(p,-r,y,i,meta);add(p,r,y,i,meta);}
    }
    private static void add(List<Part> p,int x,int y,int z,int meta) {
        if(x==0&&y==0&&z==0) throw new IllegalArgumentException("Controller position reserved");
        for(Part old:p) if(old.x==x&&old.y==y&&old.z==z) {
            if(old.meta!=meta) throw new IllegalArgumentException("Conflicting structure position");
            return;
        }
        p.add(new Part(x,y,z,meta));
    }
    public static List<Part> parts(int type) { return type>=0&&type<ALL.size()?ALL.get(type):Collections.<Part>emptyList(); }
    public static boolean validate(TileMachine tile) {
        if(tile==null||tile.getWorldObj()==null||tile.machineType()<0||tile.machineType()>5) return false;
        for(Part p:parts(tile.machineType())) {
            int x=tile.xCoord+p.x,y=tile.yCoord+p.y,z=tile.zCoord+p.z;
            if(!tile.getWorldObj().blockExists(x,y,z)) return false;
            if(p.meta<0) { if(!tile.getWorldObj().isAirBlock(x,y,z)) return false; }
            else if(tile.getWorldObj().getBlock(x,y,z)!=FluxArc.component||tile.getWorldObj().getBlockMetadata(x,y,z)!=p.meta) return false;
        }
        return true;
    }
    public static String summary(int type) {
        String[] names={"四角弯折支柱 / Y4 分段环 / 中央净空", "双侧机械臂 / 北侧扫描头 / 中央净空", "Y1 大环 / Y4 小环 / 双侧线圈", "北侧扫描门 / 两侧回收线圈", "向北上升双轨 / Y4 发射环", "9×9 场域边界 / 四角发射柱"};
        return type>=0&&type<6?names[type]:"未知结构";
    }
    private StructureSpec() {}
}
