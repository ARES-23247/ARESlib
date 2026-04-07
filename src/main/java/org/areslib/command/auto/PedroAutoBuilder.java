package org.areslib.command.auto;

import org.areslib.command.FollowPathCommand;
import org.areslib.core.localization.AresFollower;

import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

/**
 * A Pedro Pathing-specific extension of {@link AutoBuilder} that provides convenience methods
 * for following paths and path chains.
 *
 * <pre>{@code
 * Command auto = new PedroAutoBuilder(follower)
 *     .followPath(toScorePos)
 *     .runAction(new ScoreCommand(arm))
 *     .followPath(toStackPos, true)  // holdEnd
 *     .waitSeconds(0.3)
 *     .build();
 * }</pre>
 */
public class PedroAutoBuilder extends AutoBuilder {

    private final AresFollower m_follower;

    /**
     * Constructs a PedroAutoBuilder with the given follower.
     *
     * @param follower The AresFollower instance used for path following.
     */
    public PedroAutoBuilder(AresFollower follower) {
        m_follower = follower;
    }

    /**
     * Appends a path to follow.
     *
     * @param path The path to follow.
     * @return This builder for chaining.
     */
    public PedroAutoBuilder followPath(Path path) {
        return followPath(path, false);
    }

    /**
     * Appends a path to follow with hold option.
     *
     * @param path    The path to follow.
     * @param holdEnd Whether to actively hold the final position.
     * @return This builder for chaining.
     */
    public PedroAutoBuilder followPath(Path path, boolean holdEnd) {
        m_commands.add(new FollowPathCommand(m_follower, path, holdEnd));
        return this;
    }

    /**
     * Appends a path chain to follow.
     *
     * @param pathChain The path chain to follow continuously.
     * @return This builder for chaining.
     */
    public PedroAutoBuilder followPath(PathChain pathChain) {
        return followPath(pathChain, false);
    }

    /**
     * Appends a path chain to follow with hold option.
     *
     * @param pathChain The path chain to follow continuously.
     * @param holdEnd   Whether to actively hold the final position.
     * @return This builder for chaining.
     */
    public PedroAutoBuilder followPath(PathChain pathChain, boolean holdEnd) {
        m_commands.add(new FollowPathCommand(m_follower, pathChain, holdEnd));
        return this;
    }
}
