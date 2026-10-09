package dev.fluxarc.recipe;

/** An authoritative processing phase. EU is charged once per successful server tick. */
public final class Step {
    public final String name;
    public final int euPerTick;
    public final int duration;
    public Step(String name, int euPerTick, int duration) {
        if (name == null || euPerTick < 1 || duration < 1) throw new IllegalArgumentException("Invalid phase");
        this.name = name;
        this.euPerTick = euPerTick;
        this.duration = duration;
    }
}
