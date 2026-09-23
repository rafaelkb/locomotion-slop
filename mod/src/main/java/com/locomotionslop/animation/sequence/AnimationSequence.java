/*
 * Locomotion Slop - AnimationSequence
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.animation.sequence;

import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.pose.LocalSpacePose;
import com.locomotionslop.animation.util.Interpolator;
import com.locomotionslop.animation.util.TimeSpan;
import com.locomotionslop.animation.util.Timeline;
import com.locomotionslop.resource.SlopResources;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A keyframed animation: per-joint translation/rotation/scale/visibility timelines plus
 * optional custom attribute timelines and time markers.
 *
 * <p>This is the exact on-disk format used by Locomotion, so Trainguy's exported animation
 * files load unchanged.</p>
 */
public record AnimationSequence(
        Map<String, Timeline<Vector3f>> translationTimelines,
        Map<String, Timeline<Quaternionf>> rotationTimelines,
        Map<String, Timeline<Vector3f>> scaleTimelines,
        Map<String, Timeline<Boolean>> visibilityTimelines,
        Map<String, Timeline<Float>> customAttributeTimelines,
        Map<String, List<TimeSpan>> timeMarkers,
        ResourceLocation jointSkeletonLocation,
        TimeSpan length
) {

    /**
     * Samples the pose of this sequence at {@code time}.
     */
    public LocalSpacePose samplePose(JointSkeleton jointSkeleton, float time, boolean looping) {
        LocalSpacePose pose = LocalSpacePose.of(jointSkeleton);
        for (String joint : jointSkeleton.getJoints()) {
            Timeline<Vector3f> translation = this.translationTimelines.get(joint);
            Timeline<Quaternionf> rotation = this.rotationTimelines.get(joint);
            Timeline<Vector3f> scale = this.scaleTimelines.get(joint);
            Timeline<Boolean> visibility = this.visibilityTimelines.get(joint);
            if (translation == null || rotation == null || scale == null || visibility == null) {
                continue;
            }
            pose.setJointChannel(joint, JointChannel.ofTranslationRotationScaleQuaternion(
                    translation.getValueAtTime(time, looping),
                    rotation.getValueAtTime(time, looping),
                    scale.getValueAtTime(time, looping),
                    visibility.getValueAtTime(time, looping)
            ));
        }
        this.customAttributeTimelines.forEach((attribute, timeline) ->
                pose.loadCustomAttributeValue(attribute, timeline.getValueAtTime(time)));
        return pose;
    }

    public boolean containsTimelinesForJoint(String joint) {
        return this.translationTimelines.containsKey(joint)
                && this.rotationTimelines.containsKey(joint)
                && this.scaleTimelines.containsKey(joint)
                && this.visibilityTimelines.containsKey(joint);
    }

    public Set<String> getMarkersInRange(TimeSpan start, TimeSpan end, boolean looped) {
        float startSeconds = looped ? start.inSeconds() % this.length.inSeconds() : start.inSeconds();
        float endSeconds = looped ? end.inSeconds() % this.length.inSeconds() : end.inSeconds();
        Set<String> markersToReturn = new HashSet<>();
        this.timeMarkers.forEach((identifier, times) -> times.forEach(markerTime -> {
            float markerTimeSeconds = markerTime.inSeconds();
            boolean wrappedAroundLoop = endSeconds < startSeconds;
            if (wrappedAroundLoop && (markerTimeSeconds <= endSeconds || markerTimeSeconds > startSeconds)) {
                markersToReturn.add(identifier);
            } else if (markerTimeSeconds > startSeconds && markerTimeSeconds <= endSeconds) {
                markersToReturn.add(identifier);
            }
        }));
        return markersToReturn;
    }

    /**
     * Fills in identity timelines for joints the file did not animate, so sampling never has to
     * null-check.
     */
    public AnimationSequence getBaked() {
        Builder baked = AnimationSequence.builder(this.length, this.jointSkeletonLocation);
        JointSkeleton jointSkeleton = SlopResources.getOrThrowJointSkeleton(this.jointSkeletonLocation);
        for (String joint : jointSkeleton.getJoints()) {
            if (this.translationTimelines.containsKey(joint)) {
                baked.putJointTranslationTimeline(joint, this.translationTimelines.get(joint));
                baked.putJointRotationTimeline(joint, this.rotationTimelines.get(joint));
                baked.putJointScaleTimeline(joint, this.scaleTimelines.get(joint));
                baked.putJointVisibilityTimeline(joint, this.visibilityTimelines.get(joint));
            } else {
                baked.putJointTranslationTimeline(joint, Timeline.of(Interpolator.VECTOR_FLOAT, this.length.inSeconds())
                        .addKeyframe(0.0F, new Vector3f(0.0F, 0.0F, 0.0F)));
                baked.putJointRotationTimeline(joint, Timeline.of(Interpolator.QUATERNION, this.length.inSeconds())
                        .addKeyframe(0.0F, new Quaternionf()));
                baked.putJointScaleTimeline(joint, Timeline.of(Interpolator.VECTOR_FLOAT, this.length.inSeconds())
                        .addKeyframe(0.0F, new Vector3f(1.0F, 1.0F, 1.0F)));
                baked.putJointVisibilityTimeline(joint, Timeline.of(Interpolator.BOOLEAN_KEYFRAME, this.length.inSeconds())
                        .addKeyframe(0.0F, true));
            }
        }
        this.timeMarkers.forEach((marker, times) -> times.forEach(time -> baked.putTimeMarker(marker, time)));
        for (String customAttribute : jointSkeleton.getCustomAttributeDefaults().keySet()) {
            if (this.customAttributeTimelines.containsKey(customAttribute)) {
                baked.putCustomAttributeTimeline(customAttribute, this.customAttributeTimelines.get(customAttribute));
            }
        }
        return baked.build();
    }

    public static Builder builder(TimeSpan length, ResourceLocation jointSkeletonLocation) {
        return new Builder(length, jointSkeletonLocation);
    }

    public static class Builder {

        private final Map<String, Timeline<Vector3f>> translationTimelines = new HashMap<>();
        private final Map<String, Timeline<Quaternionf>> rotationTimelines = new HashMap<>();
        private final Map<String, Timeline<Vector3f>> scaleTimelines = new HashMap<>();
        private final Map<String, Timeline<Boolean>> visibilityTimelines = new HashMap<>();
        private final Map<String, Timeline<Float>> customAttributeTimelines = new HashMap<>();
        private final Map<String, List<TimeSpan>> timeMarkers = new HashMap<>();
        private final TimeSpan length;
        private final ResourceLocation jointSkeletonLocation;

        private Builder(TimeSpan length, ResourceLocation jointSkeletonLocation) {
            this.length = length;
            this.jointSkeletonLocation = jointSkeletonLocation;
        }

        public Builder putJointTranslationTimeline(String joint, Timeline<Vector3f> timeline) {
            this.translationTimelines.put(joint, timeline);
            return this;
        }

        public Builder putJointRotationTimeline(String joint, Timeline<Quaternionf> timeline) {
            this.rotationTimelines.put(joint, timeline);
            return this;
        }

        public Builder putJointScaleTimeline(String joint, Timeline<Vector3f> timeline) {
            this.scaleTimelines.put(joint, timeline);
            return this;
        }

        public Builder putJointVisibilityTimeline(String joint, Timeline<Boolean> timeline) {
            this.visibilityTimelines.put(joint, timeline);
            return this;
        }

        public Builder putCustomAttributeTimeline(String attribute, Timeline<Float> timeline) {
            this.customAttributeTimelines.put(attribute, timeline);
            return this;
        }

        public Builder putTimeMarker(String marker, TimeSpan time) {
            this.timeMarkers.computeIfAbsent(marker, key -> new ArrayList<>()).add(time);
            return this;
        }

        public AnimationSequence build() {
            return new AnimationSequence(
                    Map.copyOf(this.translationTimelines),
                    Map.copyOf(this.rotationTimelines),
                    Map.copyOf(this.scaleTimelines),
                    Map.copyOf(this.visibilityTimelines),
                    Map.copyOf(this.customAttributeTimelines),
                    Map.copyOf(this.timeMarkers),
                    this.jointSkeletonLocation,
                    this.length
            );
        }
    }
}
