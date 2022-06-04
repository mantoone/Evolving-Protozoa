package neat;

import com.google.common.collect.Streams;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by dylan on 26/05/2017.
 */
public class Neuron implements Comparable<Neuron>, Serializable {

    public interface Activation extends Function<Float, Float>, Serializable {

        Activation SIGMOID = z -> 1 / (1 + (float) Math.exp(-z));
        Activation LINEAR = z -> z;
        Activation TANH = x -> (float) Math.tanh(x);

    }

    public enum Type implements Serializable {
        SENSOR("SENSOR"), HIDDEN("HIDDEN"), OUTPUT("OUTPUT");

        private final String value;
        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final long serialVersionUID = 1L;

    private List<Neuron> inputs;
    private List<Float> weights;
    private Type type;
    private int id;
    private float state = 0;
    private float nextState = 0;
    private float learningRate = 0;
    private Activation activation = Activation.TANH;

    public Neuron(int id, Type type, Activation activation)
    {
        this(id, new ArrayList<>(), new ArrayList<>());
        this.type = type;
        this.activation = activation;
    }

    public Neuron(int id, List<Neuron> inputs, List<Float> weights)
    {
        this.id = id;
        this.inputs = inputs;
        this.weights = weights;
    }

    void tick()
    {
        nextState = 0.0f;
        for (int i = 0; i < inputs.size(); i++)
            nextState += inputs.get(i).getState() * weights.get(i);
        nextState = activation.apply(nextState);
    }

    void update()
    {
        state = nextState;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Neuron)
            return ((Neuron) o).getId() == id;
        return false;
    }

    public int getId() {
        return id;
    }

    public float getState() {
        return state;
    }

    public Neuron setState(float s) {
        state = s;
        return this;
    }

    public Neuron setActivation(Neuron.Activation activation) {
        this.activation = activation;
        return this;
    }

    public void addInput(Neuron in, Float weight) {
        inputs.add(in);
        weights.add(weight);
    }

    public Neuron setInputs(List<Neuron> inputs) {
        this.inputs = inputs;
        return this;
    }

    public List<Neuron> getInputs() {
        return inputs;
    }

    public Neuron setWeights(List<Float> weights) {
        this.weights = weights;
        return this;
    }

    public List<Float> getWeights() {
        return weights;
    }

    private float getLearningRate() {
        return learningRate;
    }

    private Neuron setLearningRate(float lr) {
        this.learningRate = lr;
        return this;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public int compareTo(Neuron o) {
        return Comparator.comparingInt(Neuron::getId).compare(this, o);
    }

    @Override
    public String toString()
    {
        String connections = String.format("[%s]",
                Streams.zip(inputs.stream(), weights.stream(),
                        (i, w) -> String.format("(%d, %.1f)", i.getId(), w))
                        .collect(Collectors.joining(", ")));
        return String.format("id:%d, state:%.1f, inputs:%s", id, state, connections);
    }
}
