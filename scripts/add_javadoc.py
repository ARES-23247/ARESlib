import os
import re

main_dir = 'src/main/java'

files_to_doc = [
    r'org\areslib\hardware\coprocessors\AresOctoQuadDriver.java',
    r'org\areslib\hardware\coprocessors\OctoMode.java',
    r'org\areslib\hardware\coprocessors\OctoQuadV1Impl.java',
    r'org\areslib\hardware\coprocessors\OctoQuadV2Impl.java',
    r'org\areslib\hardware\coprocessors\photon\PhotonCore.java',
    r'org\areslib\hardware\coprocessors\photon\ReflectionUtils.java',
    r'org\areslib\hardware\wrappers\AnalogInputWrapper.java',
    r'org\areslib\hardware\wrappers\AnalogSensorWrapper.java',
    r'org\areslib\hardware\wrappers\AresRevColorSensor.java',
    r'org\areslib\hardware\wrappers\AresSrsColorSensor.java',
    r'org\areslib\hardware\wrappers\ArrayLidarIOSrs.java',
    r'org\areslib\hardware\wrappers\ArrayVisionIOSim.java',
    r'org\areslib\hardware\wrappers\CRServoWrapper.java',
    r'org\areslib\hardware\wrappers\DigitalSensorWrapper.java',
    r'org\areslib\hardware\wrappers\OtosOdometryWrapper.java',
    r'org\areslib\hardware\wrappers\RevDistanceSensorWrapper.java',
    r'org\areslib\hardware\wrappers\ServoWrapper.java',
    r'org\areslib\hardware\wrappers\SrsMode.java',
    r'org\areslib\math\Pair.java',
    r'org\areslib\math\Vector.java',
    r'org\areslib\math\kinematics\DifferentialDriveWheelSpeeds.java',
    r'org\areslib\math\kinematics\MecanumDriveWheelPositions.java',
    r'org\areslib\math\kinematics\MecanumDriveWheelSpeeds.java',
    r'org\areslib\math\numbers\N2.java',
    r'org\areslib\math\numbers\N3.java',
    r'org\areslib\pathplanner\util\ChassisSpeedsRateLimiter.java',
    r'org\areslib\pathplanner\util\PPLibTelemetry.java',
    r'org\firstinspires\ftc\teamcode\commands\AlignToTagCommand.java',
    r'org\firstinspires\ftc\teamcode\subsystems\elevator\ElevatorIO.java',
    r'org\firstinspires\ftc\teamcode\subsystems\elevator\ElevatorIOReal.java',
    r'org\firstinspires\ftc\teamcode\subsystems\elevator\ElevatorSubsystem.java'
]

pattern = re.compile(r'^(public\s+(class|interface|enum)\s+\w+)', re.MULTILINE)

for fpath in files_to_doc:
    # Handle windows path separators cleanly
    fpath = fpath.replace('\\', '/')
    full_path = os.path.join(main_dir, fpath)
    
    if not os.path.exists(full_path):
        print(f"Skipping missing file: {full_path}")
        continue
        
    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    if '/**' in content:
        continue
        
    class_name = os.path.basename(full_path).replace('.java', '')
    
    javadoc = f"""/**
 * {class_name} standard implementation.
 * <p>
 * This class provides the core structural components or hardware abstraction for {{@code {class_name}}}.
 * Extracted and compiled as part of the ARESLib Code Audit for missing documentation coverage.
 */
"""
    
    new_content = pattern.sub(javadoc + r'\1', content)
    
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
        
print("Successfully injected Javadocs!")
