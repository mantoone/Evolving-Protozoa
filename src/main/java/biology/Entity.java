package biology;

import java.awt.Color;
import java.awt.Graphics;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import core.Settings;
import core.Simulation;
import utils.Vector2;

public abstract class Entity implements Serializable
{
	private static final long serialVersionUID = -4333766895269415282L;
	private Vector2 pos;
	private double radius;
	private double direction = 0;
	private double speed = 0;
	private double interactAngle = Math.PI / 8;
	private Color healthyColour, fullyDegradedColour;
	
	private double thinkTime = 0;
	private double maxThinkTime;
	private double timeAlive = 0;
	private double health = 1;
	private double growthRate = 0.0;
	
	private boolean dead = false;
	private double nutrition;
	private double crowdingFactor;
	protected boolean hasHandledDeath = false;

	public Entity()
	{
		healthyColour = new Color(255, 255, 255);
	}
	
	public Stream<Entity> update(double delta, Stream<Entity> entities) {
		timeAlive += delta;
		crowdingFactor = 0;
		grow(delta);
		move(delta);
		return entities.flatMap(e -> interact(e, delta));
	}

	public void grow(double delta) {
		setRadius(getRadius() * (1 + getGrowthRate() * delta));
	}

	public void setGrowthRate(double gr) {
		growthRate = gr;
	}

	public double getGrowthRate() {
		return growthRate;
	}
	
	public void render(Graphics g)
	{
		g.setColor(getColor());
		g.fillOval(
			(int) (getPos().getX() - radius),
			(int) (getPos().getY() - radius),
			(int) (2*radius),
			(int) (2*radius)
		);
	}

	public boolean tick(double delta)
	{
		thinkTime += delta;

		if (thinkTime >= maxThinkTime) {
			thinkTime = 0;
			return true;
		}
		return false;
	}
	
	public boolean isCollidingWith(Entity other)
	{
		double dist = getPos().sub(other.getPos()).len2();
		return dist < Math.pow(getRadius() + other.getRadius(), 2);
	}
	
	protected boolean isTouching(Entity other)
	{
		Vector2 ds = other.getPos().sub(getPos());
		return 0.95 * ds.len() <= getRadius() + other.getRadius();
	}
	
	public abstract boolean isEdible();

	public Stream<Entity> interact(Entity e, double delta) {
		handlePotentialCollision(e);
		return Stream.empty();
	}

	public double getCrowdingFactor() {
		return crowdingFactor;
	}

	public void handlePotentialCollision(Entity e) {

		Vector2 dx = getPos().sub(e.getPos());

		if (dx.len2() < Math.pow(3 * getRadius(), 2)) {
			crowdingFactor += e.getRadius() / (getRadius() + dx.len2());
		}

		if (isCollidingWith(e))
		{
			if (e.getVel().len()*e.getRadius() > getVel().len()*getRadius())
				setPos(e.getPos().add(dx.setLength(e.getRadius() + getRadius())));
			else
				e.setPos(getPos().sub(dx.setLength(e.getRadius() + getRadius())));
		}
	}

	public void move(double delta)
	{
		setPos(getPos().add(getVel().mul(delta)));
	}

	/**
	 * @param p maximum proportion of colour that can be lost
	 * @return how much colour to loose
	 */
	public double getColourDecay(double p) {
		return 1 + p * (health - 1);
	}

	public void setHealth(double h)
	{
		health = h;
		if (health > 1) 
			health = 1;
		
		if (health < 0.1)
			setDead(true);

	}

	public Stream<Entity> handleDeath() {
		hasHandledDeath = true;
		return Stream.empty();
	}

	public void setMaxThinkTime(double maxThinkTime) {
		this.maxThinkTime = maxThinkTime;
	}

	public abstract String getPrettyName();

	public HashMap<String, Double> getStats() {
		HashMap<String, Double> stats = new HashMap<>();
		stats.put("Age", 100 * timeAlive);
		stats.put("Health", 100 * getHealth());
		stats.put("Size", Settings.statsDistanceScalar * getRadius());
		stats.put("Speed", Settings.statsDistanceScalar * getSpeed());
		stats.put("Crowding Factor", getCrowdingFactor());
		return stats;
	}

	public HashMap<String, Double> getDebugStats() {
		HashMap<String, Double> stats = new HashMap<>();
		stats.put("Position X", getPos().getX());
		stats.put("Position Y", getPos().getY());
		stats.put("Crowding Factor", getCrowdingFactor());
		return stats;
	}
	
	public double getHealth() 
	{
		return health;
	}
	
	public Vector2 getPos() {
		return pos;
	}

	public void setPos(Vector2 pos) {
		this.pos = pos;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public boolean isDead() {
		return dead;
	}

	public void setDead(boolean dead) {
		this.dead = dead;
	}

	public Vector2 getDir() {
		return new Vector2(Math.cos(direction), Math.sin(direction));
	}

	public void setDir(Vector2 dir) {
		this.direction = dir.angle();
	}

	public void rotate(double theta) {
		this.direction += theta;
	}

	public Vector2 getVel() {
		return getDir().mul(speed);
	}

	public void setVel(Vector2 vel) {
		this.speed = vel.len();
		if (this.speed != 0)
			setDir(vel.mul(1 / this.speed));
	}

	public void setSpeed(double speed) { this.speed = speed; }

	public double getSpeed() { return speed; }

	public Color getColor() {
		Color healthyColour = getHealthyColour();
		Color degradedColour = getFullyDegradedColour();
		return new Color(
			(int) (healthyColour.getRed() + (1 - health) * (degradedColour.getRed() - healthyColour.getRed())),
			(int) (healthyColour.getGreen() + (1 - health) * (degradedColour.getGreen() - healthyColour.getGreen())),
			(int) (healthyColour.getBlue() + (1 - health) * (degradedColour.getBlue() - healthyColour.getBlue()))
		);
	}

	public Color getHealthyColour() {
		return healthyColour;
	}

	public void setHealthyColour(Color healthyColour) {
		this.healthyColour = healthyColour;
	}

	public void setDegradedColour(Color fullyDegradedColour) {
		this.fullyDegradedColour = fullyDegradedColour;
	}

	public Color getFullyDegradedColour() {
		if (fullyDegradedColour == null) {
			Color healthyColour = getHealthyColour();
			int r = healthyColour.getRed();
			int g = healthyColour.getGreen();
			int b = healthyColour.getBlue();
			double p = 0.7;
			return new Color((int) (r*p), (int) (g*p), (int) (b*p));
		}
		return fullyDegradedColour;
	}

	public double getNutrition() {
		return nutrition;
	}

	public void setNutrition(double nutrition) {
		this.nutrition = nutrition;
	}

	protected Stream<Entity> burst(Function<Double, ? extends Entity> createChild) {
		setDead(true);
		hasHandledDeath = true;

		double angle = 2 * Math.PI * Simulation.RANDOM.nextDouble();
		int nChildren = 2 + Simulation.RANDOM.nextInt(3);

		ArrayList<Entity> children = new ArrayList<>();
		for (int i = 0; i < nChildren; i++) {
			Vector2 dir = new Vector2(Math.cos(angle), Math.sin(angle));
			double p = 0.3 + 0.7 * Simulation.RANDOM.nextDouble() / nChildren;
			Entity e = createChild.apply(getRadius() * p);
			e.setPos(getPos().add(dir.mul(2*e.getRadius())));
			children.add(e);
			angle += 2 * Math.PI / nChildren;
		}

		return children.stream();
	}
}
