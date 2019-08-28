package fe.igscraper.mediaplayer;

import javafx.beans.property.*;
import javafx.scene.media.*;
import javafx.beans.value.*;
import javafx.util.*;

public class Player {
    private double sum;
    private int count;
    private BooleanProperty finished = new SimpleBooleanProperty();

    public Player play(String path) {
        MediaPlayer mp = new MediaPlayer(new Media(path));
        mp.currentTimeProperty().addListener((v, oldTime, newTime) -> {
            this.sum += newTime.subtract(oldTime).toMillis();
            ++this.count;
            if (this.sum / this.count > mp.getStopTime().subtract(newTime).toMillis()) {
                this.finished.set(true);
            }
        });
        mp.play();
        return this;
    }

    public BooleanProperty finishedProperty() {
        return this.finished;
    }
}
