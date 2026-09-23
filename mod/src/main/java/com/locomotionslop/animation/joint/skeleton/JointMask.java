package com.locomotionslop.animation.joint.skeleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-joint blend weights, used to layer animations on top of each other - for example the
 * first-person hand layer only writes to the arm/hand/item joints so the movement layer keeps
 * control of the camera and arm buffer.
 */
public final class JointMask {

    /** A mask that affects every joint and every custom attribute. */
    public static final JointMask ALL = new JointMask(null, Map.of());

    private final Set<String> joints;
    private final Map<String, Float> customAttributes;

    private JointMask(Set<String> joints, Map<String, Float> customAttributes) {
        this.joints = joints;
        this.customAttributes = customAttributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean affectsEverything() {
        return this.joints == null;
    }

    /**
     * @return 1 when the joint is in the mask, 0 when it is not.
     */
    public float jointWeight(String joint) {
        if (this.joints == null) {
            return 1.0F;
        }
        return this.joints.contains(joint) ? 1.0F : 0.0F;
    }

    /**
     * @return the mask weight for a custom attribute, defaulting to 1 when unspecified.
     */
    public float customAttributeWeight(String attribute) {
        return this.customAttributes.getOrDefault(attribute, 1.0F);
    }

    public static class Builder {

        private final Set<String> joints = new HashSet<>();
        private final Map<String, Float> customAttributes = new HashMap<>();

        public Builder includeJoint(String joint) {
            this.joints.add(joint);
            return this;
        }

        public Builder includeJoints(Iterable<String> joints) {
            for (String joint : joints) {
                this.joints.add(joint);
            }
            return this;
        }

        public Builder setCustomAttributeWeight(String attribute, float weight) {
            this.customAttributes.put(attribute, weight);
            return this;
        }

        public JointMask build() {
            return new JointMask(Set.copyOf(this.joints), Map.copyOf(this.customAttributes));
        }
    }
}
