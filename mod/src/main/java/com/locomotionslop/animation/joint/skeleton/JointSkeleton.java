/*
 * Locomotion Slop - JointSkeleton
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.animation.joint.skeleton;

import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.PartPoseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A joint hierarchy plus per-joint reference poses and mirror partners.
 */
public class JointSkeleton {

    private final Map<String, JointConfiguration> joints;
    private final String rootJoint;
    private final Map<String, Float> customAttributeDefaults;

    private JointSkeleton(Map<String, JointConfiguration> joints, String rootJoint, Map<String, Float> customAttributeDefaults) {
        this.joints = joints;
        this.rootJoint = rootJoint;
        this.customAttributeDefaults = customAttributeDefaults;
    }

    public static Builder of(String rootJoint) {
        return new Builder(rootJoint);
    }

    public Map<String, Float> getCustomAttributeDefaults() {
        return new HashMap<>(this.customAttributeDefaults);
    }

    public Set<String> getCustomAttributes() {
        return this.customAttributeDefaults.keySet();
    }

    public boolean containsCustomAttribute(String name) {
        return this.customAttributeDefaults.containsKey(name);
    }

    public List<String> getDirectChildrenOfJoint(String joint) {
        return this.joints.get(joint).children();
    }

    public boolean jointIsParentOfChild(String parent, String child) {
        return Objects.equals(this.joints.get(child).parent(), parent);
    }

    public String getRootJoint() {
        return this.rootJoint;
    }

    public Set<String> getJoints() {
        return this.joints.keySet();
    }

    public JointConfiguration getJointConfiguration(String joint) {
        return this.joints.get(joint);
    }

    public boolean containsJoint(String joint) {
        return this.joints.containsKey(joint);
    }

    public static class Builder {

        private final Map<String, JointConfiguration> joints;
        private final String rootJoint;
        private final Map<String, Float> customAttributeDefaults;

        protected Builder(String rootJoint) {
            this.joints = new HashMap<>();
            this.rootJoint = rootJoint;
            this.customAttributeDefaults = new HashMap<>();
        }

        public Builder defineJoint(String jointName, JointConfiguration jointConfiguration) {
            this.joints.put(jointName, jointConfiguration);
            return this;
        }

        public Builder defineCustomAttribute(String name, float defaultValue) {
            this.customAttributeDefaults.put(name, defaultValue);
            return this;
        }

        public JointSkeleton build() {
            return new JointSkeleton(this.joints, this.rootJoint, this.customAttributeDefaults);
        }
    }

    public record JointConfiguration(
            String parent,
            List<String> children,
            JointChannel referencePose,
            String mirrorJoint,
            String modelPartIdentifier,
            PartPoseData modelPartOffset,
            String modelPartSpaceParent
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String parent;
            private final List<String> children;
            private JointChannel referencePose = JointChannel.ZERO;
            private String mirrorJoint;
            private String modelPartIdentifier;
            private PartPoseData modelPartOffset = PartPoseData.IDENTITY;
            private String modelPartSpaceParent;

            private Builder() {
                this.parent = null;
                this.children = new ArrayList<>();
                this.mirrorJoint = null;
                this.modelPartIdentifier = null;
            }

            public Builder addChild(String child) {
                this.children.add(child);
                return this;
            }

            public Builder setParent(String parent) {
                this.parent = parent;
                return this;
            }

            public Builder setReferencePose(JointChannel referencePose) {
                this.referencePose = referencePose;
                return this;
            }

            public Builder setMirrorJoint(String mirrorJoint) {
                this.mirrorJoint = mirrorJoint;
                return this;
            }

            public Builder setModelPartIdentifier(String modelPartIdentifier) {
                this.modelPartIdentifier = modelPartIdentifier;
                return this;
            }

            public Builder setModelPartOffset(PartPoseData modelPartOffset) {
                this.modelPartOffset = modelPartOffset;
                return this;
            }

            public Builder setModelPartSpaceParent(String modelPartSpaceParent) {
                this.modelPartSpaceParent = modelPartSpaceParent;
                return this;
            }

            public JointConfiguration build() {
                return new JointConfiguration(
                        this.parent,
                        this.children,
                        this.referencePose,
                        this.mirrorJoint,
                        this.modelPartIdentifier,
                        this.modelPartOffset,
                        this.modelPartSpaceParent
                );
            }
        }
    }
}
