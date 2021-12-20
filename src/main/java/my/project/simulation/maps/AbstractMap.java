package my.project.simulation.maps;

import my.project.gui.simulation.visualization.GridBuilder;
import my.project.simulation.data.structures.PrefixTree;
import my.project.simulation.utils.IObserver;
import my.project.simulation.enums.MapArea;
import my.project.simulation.sprites.*;
import my.project.simulation.utils.EnergyComparator;
import my.project.simulation.utils.Random;
import my.project.simulation.utils.Vector2D;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMap implements IMap, IObserver {
    private static final double MIN_BREED_ENERGY_RATIO = .5;
    private final int moveEnergy;
    private final int startEnergy;
    private final int bushEnergy;
    private final int grassEnergy;

    protected final Vector2D mapLowerLeft = new Vector2D(0, 0);
    protected final Vector2D mapUpperRight;
    protected final Vector2D jungleLowerleft;
    protected final Vector2D jungleUpperRight;

    protected final Map<Vector2D, SortedSet<Animal>> mapAnimals = new HashMap<>();
    protected final Map<Vector2D, AbstractPlant> mapPlants = new HashMap<>();
    protected final Set<AbstractPlant> eatenPlants = new HashSet<>();
    protected final PrefixTree<Integer, Animal> genotypesTree = new PrefixTree<>(Animal.getPossibleGenes());

    private final int initialAnimalsCount;

    private int animalsCount;
    private int animalsDiedCount = 0;
    private int plantsCount = 0;
    private GridBuilder gridBuilder;

    // TODO - add input type checking (on a frontEnd side)
    AbstractMap(int width, int height, double jungleRatio,
                int startEnergy, int moveEnergy, int bushEnergy, int grassEnergy,
                int animalsCount) {
        // Store initial values
        this.initialAnimalsCount = animalsCount;
        this.startEnergy = startEnergy;
        this.moveEnergy = moveEnergy;
        this.bushEnergy = bushEnergy;
        this.grassEnergy = grassEnergy;

        // Calculate map upper-right bound vector
        this.mapUpperRight = new Vector2D(width - 1, height - 1);

        // Calculate jungle bounding vectors
        int jungleWidth = (int)Math.round(width * jungleRatio);
        if (jungleWidth % 2 != width % 2) jungleWidth += 1;
        int jungleHeight = (int)Math.round(height * jungleRatio);
        if (jungleHeight % 2 != height % 2) jungleHeight += 1;
        this.jungleLowerleft = new Vector2D((width - jungleWidth) / 2, (height - jungleHeight) / 2);
        this.jungleUpperRight = this.jungleLowerleft.add(new Vector2D(jungleWidth - 1, jungleHeight - 1));
    }

    @Override
    public void changeSpritePosition(ISprite sprite) throws IllegalArgumentException, NoSuchElementException {
        if (!(sprite instanceof Animal animal)) {
            throw new IllegalArgumentException("Cannot change position of a sprite which is not an animal");
        } else if (animal.getPosition() != animal.getPrevPosition()) {
            // Remove an animal from the previous position on a map
            System.out.println("Calling removeAnimal from changeSpritePosition");
            System.out.println("Moving animal from: " + animal.getPrevPosition() + " to: " + animal.getPosition());
            removeAnimal(animal, animal.getPrevPosition());
            // Add an animal at a new position on the map
            placeAnimal(animal);
        }
    }

    @Override
    public void removeSprite(ISprite sprite) throws NoSuchElementException {
        // If a sprite is an animal object
        if (sprite instanceof Animal animal) {
            System.out.println("\n>>> Calling removeAnimal from removeSprite");
            removeAnimal(animal, animal.getPosition());
            animalsDiedCount++;
            genotypesTree.remove(animal.getGenome());
        }
        // If a sprite is a plant object
        else if (sprite instanceof AbstractPlant) {
            removePlant((AbstractPlant) sprite);
            plantsCount--;
        }
    }

    @Override
    public void addSprite(ISprite sprite) throws IllegalArgumentException {
        // If a sprite is an animal object
        if (sprite instanceof Animal animal) {
            placeAnimal(animal);
            animalsCount++;
            genotypesTree.insert(animal.getGenome(), animal);
            System.out.println("Inserted genome: " + animal.getGenome());
            System.out.println(genotypesTree.getMaxCountValues());
            System.out.println(genotypesTree.getValues(animal.getGenome()));
            System.out.println("\n\n\n");
        }
        // If a sprite is a plant object
        else if (sprite instanceof AbstractPlant) {
            placePlant((AbstractPlant) sprite);
            plantsCount++;
        }
    }

    @Override
    public MapArea getAreaType(Vector2D position) {
        if (position.precedes(jungleUpperRight) && position.follows(jungleLowerleft)) return MapArea.JUNGLE;
        else if (position.precedes(mapUpperRight) && position.follows(mapLowerLeft))  return MapArea.STEPPE;
        return null;
    }

    @Override
    public void setGridBuilder(GridBuilder gridBuilder) {
        this.gridBuilder = gridBuilder;
    }

    @Override
    public GridBuilder getGridBuilder() {
        return gridBuilder;
    }

    @Override
    public void spawnPlants() {
        spawnSinglePlant(MapArea.JUNGLE);
        spawnSinglePlant(MapArea.STEPPE);
    }

    @Override
    public long getNewAnimalID() {
        return animalsCount + 1;
    }

    @Override
    public void update() {
        updateAnimals();
        feedAnimals();
        breedAnimals();
        spawnPlants();
    }

    public List<Vector2D> getMapBoundingRect() {
        return new ArrayList<>() {{
            add(mapLowerLeft);
            add(mapUpperRight);
        }};
    }

    public List<List<Integer>> getDominantGenotypes() {
        return genotypesTree.getMaxCountKeys();
    }

    public Set<Animal> getDominantGenotypesAnimals() {
        return genotypesTree.getMaxCountValues();
    }

    public int getMoveEnergy() {
        return moveEnergy;
    }

    public int getStartEnergy() {
        return startEnergy;
    }

    public int getMinBreedEnergy() {
        return (int)(startEnergy * MIN_BREED_ENERGY_RATIO + .5);
    }

    public void initialize() {
        randomlyPalceAnimals(initialAnimalsCount);
        spawnPlants();
    }

    private void randomlyPalceAnimals(int animalsCount) {
        int minX = mapLowerLeft.getX();
        int maxX = mapUpperRight.getX();
        int minY = mapLowerLeft.getY();
        int maxY = mapUpperRight.getY();

        for (int i = 0; i < animalsCount; i++) {
            Vector2D randomPosition = Vector2D.randomVector(minX, maxX, minY, maxY);
            Vector2D position = getSegmentEmptyFieldVector(randomPosition, mapLowerLeft, mapUpperRight);
            Animal animal = new Animal(this, position);
            animal.add();
        }
    }

    private void placeAnimal(Animal animal) {
        Vector2D position = animal.getPosition();
        System.out.println("Placing animal " + animal.getID() + " at " + position);
        // Create the new animals list if there is no animals list on the specified position
        if (mapAnimals.get(position) == null) mapAnimals.put(position, new TreeSet<>(new EnergyComparator()));
        // Add an animal to the list
        mapAnimals.get(position).add(animal);
        // Add eaten plants to the list awaiting update
        AbstractPlant plant = mapPlants.get(position);
        if (plant != null) eatenPlants.add(plant);
        System.out.println("ANIMAL PLACED");
    }

    private void removeAnimal(Animal animal, Vector2D position) throws NoSuchElementException {
        Set<Animal> animals = mapAnimals.get(position);
        System.out.println("In removeAnimal trying to remove with id: " + animal.getID() + " from position " + position);
        System.out.println("Animal current position: " + animal.getPosition() + ", Animal previous position: " + animal.getPrevPosition());
        System.out.println("Animal current direction: " + animal.getDirection());
        System.out.println("All animals at: " + position + ": " + animals.stream().map(Animal::getID).collect(Collectors.toList()));
        System.out.println("All animals at: " + animal.getPrevPosition() + ": " + mapAnimals.get(animal.getPrevPosition()).stream().map(Animal::getID).collect(Collectors.toList()));
        boolean found = false;
        for (Vector2D v: mapAnimals.keySet()) {
            for (Animal b: mapAnimals.get(v)) {
                if (b.getID() == animal.getID()) {
                    found = true;
                    System.out.println("Found animal with id: " + animal.getID() + " at: " + v);
                }
            }
        }
        if (!found) System.out.println("THERE IS NO ANIMAL WITH id: " + animal.getID() + " ON A MAP (probably deleted before)");
        System.out.println("Animal positions history: " + animal.getPosHist());
        System.out.println("Is contained? " + animals.contains(animal));

//        if (animals != null && animals.contains(animal)) animals.remove(animal);
        boolean removed = false;
        Iterator<Animal> it = animals.iterator();
        while (it.hasNext()) {
            Animal animal1 = it.next();
            if (animal1.getID() == animal.getID()) { it.remove(); removed=true; }
        }
//        else {
        if (!removed) {
            System.out.println(">>>>>>>>>> ERROR <<<<<<<<<<<");
            throw new NoSuchElementException("Sprite " + animal + " is not on the map");
        }
//        }


//        Set<Animal> animalsCurrPosition = mapAnimals.get(animal.getPosition());
//        Set<Animal> animalsPrevPosition = mapAnimals.get(animal.getPrevPosition());
//
//        System.out.println("Curr animal: " + animal + " (" + animal.hashCode() + ")");
//        System.out.println("Previous position: " + animal.getPrevPosition());
//        System.out.println("Current position: " + animal.getPosition());
//        System.out.println("All animals: " + mapAnimals);
//        System.out.println("Received animalsCurrPosition: " + animalsCurrPosition);
//        if (animalsCurrPosition != null) {
//            for (Animal a : animalsCurrPosition) {
//                System.out.println(a.getID() + " the same? " + (a.getID() == animal.getID()));
//            }
//            System.out.println("Is in current? " + animalsCurrPosition.contains(animal));
//        }
//        System.out.println("Received animalsPrevPosition: " + animalsPrevPosition);
//        if (animalsPrevPosition != null) {
//            for (Animal a : animalsPrevPosition) {
//                System.out.println(a.getID() + " the same? " + (a.getID() == animal.getID()));
//            }
//            System.out.println("Is in previous? " + animalsPrevPosition.contains(animal));
//        }
//        System.out.println("\n");
//
//        if (animalsCurrPosition != null && animalsCurrPosition.contains(animal)) {
//            animalsCurrPosition.remove(animal);
//        } else if (animalsPrevPosition != null && animalsPrevPosition.contains(animal)) {
//            animalsPrevPosition.remove(animal);
//        } else {
//            System.out.println("ERROR IS NOW !!!");
//            if (animalsCurrPosition != null) {
//                for (Animal currAnimal: animalsCurrPosition) {
//                    if (currAnimal.getID() == animal.getID()) {
//                        animalsCurrPosition.remove(currAnimal);
//                        return;
//                    }
//                }
//            }
//            if (animalsPrevPosition != null) {
//                for (Animal currAnimal: animalsPrevPosition) {
//                    if (currAnimal.getID() == animal.getID()) {
//                        animalsPrevPosition.remove(currAnimal);
//                        return;
//                    }
//                }
//            }
//            throw new NoSuchElementException("Sprite " + animal + " is not on the map");
//        }
    }

    private void placePlant(AbstractPlant plant) throws IllegalArgumentException {
        Vector2D position = plant.getPosition();
        // Place plant on a field only if there is no other element occupying this field
        if (!isEmptyField(position)) {
            String message = "Cannot place plant on field: " + position + ". Field is not empty.";
            throw new IllegalArgumentException(message);
        }
        mapPlants.put(position, plant);
    }

    private void removePlant(AbstractPlant plant) throws NoSuchElementException {
        Vector2D position = plant.getPosition();
        if (mapPlants.remove(position) == null) {
            throw new NoSuchElementException("Plant " + plant + " is not on the map");
        }
    }

    private List<Animal> getAllAnimals() {
        List<Animal> result = new ArrayList<>();
        for (Set<Animal> animals: mapAnimals.values()) result.addAll(animals);
        return result;
    }

    private void updateAnimals() {
        for (Animal animal: getAllAnimals()) animal.update();
    }

    private void feedAnimals() {
        for (AbstractPlant plant: eatenPlants) {
            // From the animals on the current field, choose
            // only ones that have the most energy
            List<Animal> animals = getAnimalsWithMaxEnergy(plant.getPosition(), 1);
            // Divide the plant's energy into equal parts for
            // each selected animal
            try {
                int energyPart = plant.getEnergy() / animals.size();
                // Loop over all selected animals and feed them
                for (Animal animal: animals) animal.feed(energyPart);
            } catch (ArithmeticException e) {
                System.out.println(">>> all animals at field: " + mapAnimals.get(plant.getPosition()));
                e.printStackTrace();
            }
            plant.remove();
        }
        eatenPlants.clear();
    }

    private void breedAnimals() {
        for (Vector2D position: mapAnimals.keySet()) {
            SortedSet<Animal> animals = mapAnimals.get(position);
            // Continue if there are not enough animals to breed on one field
            if (animals.size() < 2) continue;
            // Otherwise, find 2 animals with the greatest energy value
            List<Animal> animalsToBreed = getAnimalsToBreed(position);
            // Continue if there are no enough animals on the current field
            if (animalsToBreed == null) continue;
            animalsToBreed.get(0).breed(animalsToBreed.get(1));
        }
    }

    private List<Animal> getAnimalsWithMaxEnergy(Vector2D position, int firstMaxValuesCount) {
        List<Animal> result = new ArrayList<>();
        Set<Animal> animals = mapAnimals.get(position);
        int maxEnergy = 0;
        int count = 1;
        // Animals are sorted in a non-increasing order, so a loop
        // below will always take the first animal (the one with the
        // greatest energy) and all others having the same energy value
        if (animals != null) {
            for (Animal animal: animals) {
                if (animal.getEnergy() < maxEnergy && ++count > firstMaxValuesCount) break;
                maxEnergy = animal.getEnergy();
                result.add(animal);
            }
        }
        return result;
    }

    private List<Animal> getAnimalsToBreed(Vector2D position) {
        List<Animal> animals;
        // Try to get 2 animals with the same energy equal to the maximum energy
        animals = getAnimalsWithMaxEnergy(position, 1);
        // If there are not enough animals, take 2 that have different energy
        // value but greater than all the remaining animals
        if (animals.size() < 2)  animals = getAnimalsWithMaxEnergy(position, 2);
        if (animals.size() >= 2) return Random.sample(animals, 2);
        return null;
    }

    public boolean isEmptyField(Vector2D position) {
        return (mapAnimals.get(position) == null || mapAnimals.get(position).size() == 0)
                && mapPlants.get(position) == null;
    }

    private void spawnSinglePlant(MapArea area) {
        Vector2D position = switch (area) {
            case JUNGLE -> getJungleEmptyFieldVector();
            case STEPPE -> getSteppeEmptyFieldVector();
        };
        // Do not spawn a grass object if there is no more space available
        if (position == null) return;
        // Create a plant object and add it to the map
        ISprite plant = switch (area) {
            case JUNGLE -> new Bush(this, position, bushEnergy);
            case STEPPE -> new Grass(this, position, grassEnergy);
        };
        plant.add();
    }

    private Vector2D getJungleEmptyFieldVector() {
        Vector2D initialPosition = Vector2D.randomVector(jungleLowerleft.getX(), jungleUpperRight.getX(),
                                                         jungleLowerleft.getY(), jungleUpperRight.getY());
        return getSegmentEmptyFieldVector(initialPosition, jungleLowerleft, jungleUpperRight);
    }

    private Vector2D getSteppeEmptyFieldVector() {
        /*
        *  +---+---+---+
        *  | 0 | 1 | 2 |
        *  +---+---+---+
        *  | 3 |   | 4 |
        *  +---+---+---+
        *  | 5 | 6 | 7 |
        *  +---+---+---+
        */
        int segmentsCount = 8;
        int segmentIdx = Random.randInt(segmentsCount - 1);

        Vector2D position = null;
        int count = 0;
        while (count++ < segmentsCount && position == null) {
            Vector2D segmentLowerLeft  = getSegmentLowerLeft(segmentIdx);
            Vector2D segmentUpperRight = getSegmentUpperRight(segmentIdx);
            Vector2D initialPosition;
            if (count > 0) initialPosition = segmentLowerLeft;
            else initialPosition = Vector2D.randomVector(segmentLowerLeft.getX(), segmentUpperRight.getX(),
                                                         segmentLowerLeft.getY(), segmentUpperRight.getY());
            // Continue if the area is a jungle (for example, when there is no segment with
            // specified segmentIdx, because a jungle occupies a whole (or almost whole) map)
            position = getSegmentEmptyFieldVector(initialPosition, segmentLowerLeft, segmentUpperRight);
            segmentIdx = (segmentIdx + 1) % segmentsCount;
        }
        return position;
    }

    private Vector2D getSegmentLowerLeft(int segmentIdx) throws IllegalArgumentException {
        return switch (segmentIdx) {
            case 0 -> mapLowerLeft.upperLeft(jungleUpperRight).add(new Vector2D(0, 1));
            case 1 -> jungleLowerleft.upperLeft(jungleUpperRight).add(new Vector2D(0, 1));
            case 2 -> jungleUpperRight.add(new Vector2D(1, 1));
            case 3 -> mapLowerLeft.upperLeft(jungleLowerleft);
            case 4 -> jungleLowerleft.lowerRight(jungleUpperRight).add(new Vector2D(1, 0));
            case 5 -> mapLowerLeft;
            case 6 -> mapLowerLeft.lowerRight(jungleLowerleft);
            case 7 -> mapLowerLeft.lowerRight(jungleUpperRight).add(new Vector2D(1, 0));
            default -> throw new IllegalArgumentException(segmentIdx + " is not valid segment index");
        };
    }

    private Vector2D getSegmentUpperRight(int segmentIdx) throws IllegalArgumentException {
        return switch (segmentIdx) {
            case 0 -> jungleLowerleft.upperLeft(mapUpperRight).subtract(new Vector2D(1, 0));
            case 1 -> jungleUpperRight.upperLeft(mapUpperRight);
            case 2 -> mapUpperRight;
            case 3 -> jungleLowerleft.upperLeft(jungleUpperRight).subtract(new Vector2D(1, 0));
            case 4 -> jungleUpperRight.lowerRight(mapUpperRight);
            case 5 -> jungleLowerleft.subtract(new Vector2D(1, 1));
            case 6 -> jungleUpperRight.lowerRight(jungleLowerleft).subtract(new Vector2D(0, 1));
            case 7 -> jungleLowerleft.lowerRight(mapUpperRight).subtract(new Vector2D(0, 1));
            default -> throw new IllegalArgumentException(segmentIdx + " is not valid segment index");
        };
    }

    private Vector2D getSegmentEmptyFieldVector(Vector2D position, Vector2D lowerLeft, Vector2D upperRight) {
        int minX = lowerLeft.getX();
        int maxX = upperRight.getX();
        int minY = lowerLeft.getY();
        int maxY = upperRight.getY();
        // Calculate segment dimensions
        int segmentWidth = maxX - minX + 1;
        int segmentHeight = maxY - minY + 1;
        // Return null is a segment doesn't exist
        if (segmentHeight == 0 || segmentWidth == 0) return null;
        int i = position.getX() * segmentHeight + position.getY();
        int count = 0;
        int segmentFieldsCount = segmentWidth * segmentHeight;
        // Loop over subsequent fields till a field is not empty
        while (!isEmptyField(position)) {
            i = (i + 1) % segmentFieldsCount;
            position = lowerLeft.add(new Vector2D(i / segmentHeight, i % segmentHeight));
            // Return null if cannot find an empty field in a segment
            if (count++ == segmentFieldsCount) return null;
        }
        return position;
    }
}
