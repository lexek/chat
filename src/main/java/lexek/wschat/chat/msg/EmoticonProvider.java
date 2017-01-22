package lexek.wschat.chat.msg;

import lexek.wschat.db.model.Emoticon;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;

@Contract
public interface EmoticonProvider<T extends Emoticon> {
    List<T> getEmoticons();
}
