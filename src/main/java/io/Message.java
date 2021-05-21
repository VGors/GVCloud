package io;

import java.io.Serializable;

public interface Message extends Serializable {
    Commands getCommand();
}
