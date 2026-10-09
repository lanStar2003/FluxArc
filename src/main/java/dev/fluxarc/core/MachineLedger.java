package dev.fluxarc.core;

/** Pure server-side accounting. Inventory reservation/commit surrounds begin/finish in the tile. */
public final class MachineLedger {
    public static final int IDLE=0, RUNNING=2, INVALID=5, NO_POWER=6, OUTPUT_FULL=7;
    private final long capacity;
    private long energy;
    private int progress, duration;
    private boolean active;
    private long serial;
    public MachineLedger(long capacity) { if(capacity<=0)throw new IllegalArgumentException("capacity");this.capacity=capacity; }
    public long inject(long voltage,long amperes,long maxVoltage) {
        if(voltage<=0||amperes<=0||voltage>maxVoltage)return 0;
        long accepted=Math.min(amperes,(capacity-energy)/voltage);
        energy+=accepted*voltage;return accepted;
    }
    public boolean begin(int ticks) {
        if(active||ticks<=0)return false;
        active=true;duration=ticks;progress=0;serial++;return true;
    }
    public int advance(boolean valid,long cost) {
        if(!active)return IDLE;
        if(!valid)return INVALID;
        if(progress>=duration)return OUTPUT_FULL;
        if(cost<0)throw new IllegalArgumentException("negative cost");
        if(energy<cost)return NO_POWER;
        energy-=cost;progress++;return RUNNING;
    }
    public boolean ready(){return active&&progress>=duration;}
    /** Only call after all output slots/tanks were simulated and committed on the same server tick. */
    public boolean finish(){if(!ready())return false;active=false;progress=0;duration=0;return true;}
    public void abort(){active=false;progress=0;duration=0;}
    public long energy(){return energy;} public long capacity(){return capacity;}
    public int progress(){return progress;}public int duration(){return duration;}
    public boolean active(){return active;}public long serial(){return serial;}
    public void restore(long eu,int done,int total,boolean running,long serialValue) {
        energy=Math.max(0,Math.min(capacity,eu));serial=Math.max(0,serialValue);
        active=running&&total>0&&done>=0&&done<=total;
        progress=active?done:0;duration=active?total:0;
    }
}
