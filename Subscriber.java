package blenderparallelrendering;

/**
 * Every class imlementing this interface will listen to messages coming from
 * another class.
 *
 * @author arthu
 */
public interface Subscriber {

    public void update(String message);
}
