package org.areslib.command.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import org.areslib.core.localization.AresFollower;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DynamicPathCommandTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AresFollower mockFollower;

    @Mock
    private Supplier<Translation2d> mockObstacleSupplier;

    private DynamicPathCommand dynamicPathCommand;

    @Mock
    private Follower mockPedroFollower;

    @Mock
    private com.pedropathing.paths.PathBuilder mockPathBuilder;

    @BeforeEach
    public void setup() {
        // Assume robot starts at origin (0, 0)
        when(mockFollower.getPose()).thenReturn(new Pose(0.0, 0.0, 0.0));
        when(mockFollower.getFollower()).thenReturn(mockPedroFollower);
        when(mockPedroFollower.pathBuilder()).thenReturn(mockPathBuilder);
        lenient().when(mockPathBuilder.addPath(ArgumentMatchers.<com.pedropathing.geometry.Curve>any())).thenReturn(mockPathBuilder);
        lenient().when(mockPathBuilder.setLinearHeadingInterpolation(anyDouble(), anyDouble())).thenReturn(mockPathBuilder);
        lenient().when(mockPathBuilder.setTangentHeadingInterpolation()).thenReturn(mockPathBuilder);
    }

    @Test
    public void testClearPathNoObstacle() {
        Translation2d target = new Translation2d(10.0, 0.0);
        Rotation2d targetHeading = new Rotation2d(0.0);
        when(mockObstacleSupplier.get()).thenReturn(null);

        dynamicPathCommand = new DynamicPathCommand(mockFollower, target, targetHeading, mockObstacleSupplier, 1.0, 0.5);
        dynamicPathCommand.initialize();

        verify(mockPathBuilder).addPath(ArgumentMatchers.<com.pedropathing.geometry.Curve>any());
        verify(mockPathBuilder).setLinearHeadingInterpolation(anyDouble(), anyDouble());
        verify(mockFollower).followPath(ArgumentMatchers.<com.pedropathing.paths.PathChain>any());
    }

    @Test
    public void testDodgingObstaclePath() {
        Translation2d target = new Translation2d(10.0, 0.0);
        Rotation2d targetHeading = new Rotation2d(0.0);
        when(mockObstacleSupplier.get()).thenReturn(new Translation2d(5.0, 0.0));

        dynamicPathCommand = new DynamicPathCommand(mockFollower, target, targetHeading, mockObstacleSupplier, 1.0, 0.5);
        dynamicPathCommand.initialize();

        verify(mockPathBuilder).addPath(ArgumentMatchers.<com.pedropathing.geometry.Curve>any());
        verify(mockPathBuilder).setTangentHeadingInterpolation();
        verify(mockFollower).followPath(ArgumentMatchers.<com.pedropathing.paths.PathChain>any());
    }
}
