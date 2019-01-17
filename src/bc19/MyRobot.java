package bc19;

public class MyRobot extends BCAbstractRobot {
    int step = -1;

    public Action turn() {
        step++;

        if(me.unit == SPECS.CASTLE) {

        }

        else if(me.unit == SPECS.CHURCH) {

        }

        else if(me.unit == SPECS.PILGRIM) {

        }

        else if(me.unit == SPECS.CRUSADER) {

        }

        else if(me.unit == SPECS.PROPHET) {

        }

        else if(me.unit == SPECS.PREACHER) {

        }

        else {
            log("This unit is not a knows unit");
        }
    }
}
