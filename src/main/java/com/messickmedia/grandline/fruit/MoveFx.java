package com.messickmedia.grandline.fruit;

/** The effect of a move. Return false if it could not be used (no target, wrong place), so no cost or cooldown applies. */
@FunctionalInterface
public interface MoveFx {
    boolean run(Cast c);
}
