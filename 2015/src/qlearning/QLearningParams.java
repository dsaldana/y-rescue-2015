package qlearning;

/**
 * Stores the parameters of Q-learning
 */
public class QLearningParams {
    private static QLearningParams instance;
    
    private float alpha, gamma, epsilon;
    
    private QLearningParams(){}
    
    public static QLearningParams getInstance(){
        if (instance == null) {
            instance = new QLearningParams();
        }
        return instance;
    }

    /**
     * @return the alpha
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * @param alpha the alpha to set
     */
    public QLearningParams setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * @return the gamma
     */
    public float getGamma() {
        return gamma;
    }

    /**
     * @param gamma the gamma to set
     */
    public QLearningParams setGamma(float gamma) {
        this.gamma = gamma;
        return this;
    }

    /**
     * @return the epsilon
     */
    public float getEpsilon() {
        return epsilon;
    }

    /**
     * @param epsilon the epsilon to set
     */
    public QLearningParams setEpsilon(float epsilon) {
        this.epsilon = epsilon;
        return this;
    }
}
