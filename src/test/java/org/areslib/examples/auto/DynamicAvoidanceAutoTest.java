package org.areslib.examples.auto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.areslib.core.localization.AresFollower;
import org.areslib.hardware.interfaces.ArrayLidarIO;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

public class DynamicAvoidanceAutoTest {

    private SwerveDriveSubsystem mockDrive;
    private AresFollower mockAresFollower;
    private ArrayLidarIO.ArrayLidarInputs lidarInputs;
    private Follower mockPedroFollower;

    @BeforeEach
    public void setup() {
        mockDrive = mock(SwerveDriveSubsystem.class);
        
        // Deep stub required to mock the pathBuilder().addPath().setLinearHeadingInterpolation().build() chains
        mockAresFollower = mock(AresFollower.class);
        mockPedroFollower = mock(Follower.class, Answers.RETURNS_DEEP_STUBS);
        
        when(mockAresFollower.getFollower()).thenReturn(mockPedroFollower);
        when(mockAresFollower.getPose()).thenReturn(new Pose(1.0, 0.0, 0.0));
        
        lidarInputs = new ArrayLidarIO.ArrayLidarInputs();
        // Construct standard 64-element distance array populated with maximum valid read distance (e.g. 4000mm)
        lidarInputs.distanceZonesMm = new double[64];
        for (int i = 0; i < 64; i++) {
            lidarInputs.distanceZonesMm[i] = 4000.0;
        }
    }

    @Test
    public void testExecutesNormallyWhenPathIsClear() {
        DynamicAvoidanceAuto autoCommand = new DynamicAvoidanceAuto(mockDrive, mockAresFollower, lidarInputs);
        autoCommand.initialize();
        
        // Step execution - path is clear, so NO detour triggered
        autoCommand.execute();
        
        // The breakFollowing mechanism should NEVER be called on a clear path
        verify(mockAresFollower, times(0)).breakFollowing();
    }

    @Test
    public void testTriggersDetourWhenObstacleDetected() {
        DynamicAvoidanceAuto autoCommand = new DynamicAvoidanceAuto(mockDrive, mockAresFollower, lidarInputs);
        autoCommand.initialize();
        
        // Inject an obstacle immediately inside the front center camera array (< 500mm threshold)
        // For an 8x8 grid, row 3 col 3 is index 27
        lidarInputs.distanceZonesMm[27] = 350.0; // 350mm = 0.35m
        
        // Step execution - obstacle encountered!
        autoCommand.execute();
        
        // Verify the robot halts its current execution
        verify(mockAresFollower, times(1)).breakFollowing();
        
        // Verify a new detoured PathChain is dynamically issued to the follower
        // FollowPath is called once in initialization, and once during the detour reroute
        verify(mockAresFollower, times(2)).followPath(any(PathChain.class));
    }
}
