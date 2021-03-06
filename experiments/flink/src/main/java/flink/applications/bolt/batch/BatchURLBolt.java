package flink.applications.bolt.batch;

import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import flink.applications.bolt.base.AbstractScoreBolt;
import flink.applications.model.cdr.CallDetailRecord;

import java.util.LinkedList;

/**
 * @author Maycon Viana Bordin <mayconbordin@gmail.com>
 */
public class BatchURLBolt extends AbstractScoreBolt {
    // private static final Logger LOG = Logger.getLogger(URLBolt.class);

    public BatchURLBolt() {
        super("url");
    }

    @Override
    public void execute(Tuple input) {
        //input..
        LinkedList<Values> batch_input = (LinkedList<Values>) input.getValue(0);

        //LinkedList<String> batch_caller = (LinkedList<String>) input.getValue(0);
        int batch = batch_input.size();
        //LinkedList<Long> batch_timestamp = (LinkedList<Long>) input.getValue(1);
        //LinkedList<Double> batch_rate = (LinkedList<Double>) input.getValue(2);
        //LinkedList<CallDetailRecord> batch_cdr = (LinkedList<CallDetailRecord>) input.getValue(3);

        //output.
        LinkedList<String> batch_number = new LinkedList<>();
        LinkedList<Long> batch_timestamp_output = new LinkedList<>();
        LinkedList<Double> batch_score = new LinkedList<>();
        LinkedList<CallDetailRecord> batch_cdr_output = new LinkedList<>();


        for (int i = 0; i < batch; i++) {
            Values v = batch_input.get(i);
            CallDetailRecord cdr = (CallDetailRecord) v.get(3);
            String number = (String) v.get(0);
            long timestamp = (long) v.get(1);
            double rate = (double) v.get(2);

            String key = String.format("%s:%d", number, timestamp);
            Source src = parseComponentId(input.getSourceComponent());

            if (map.containsKey(key)) {
                Entry e = map.get(key);
                e.set(src, rate);

                if (e.isFull()) {
                    // calculate the score for the ratio
                    double ratio = (e.get(Source.ENCR) / e.get(Source.ECR));
                    double score = score(thresholdMin, thresholdMax, ratio);

                    //LOG.debug(String.format("T1=%f; T2=%f; ENCR=%f; ECR=%f; Ratio=%f; Score=%f",
                    //        thresholdMin, thresholdMax, e.get(Source.ENCR), e.get(Source.ECR), ratio, score));

                    //collector.emit(new Values(number, timestamp, score, cdr));

                    batch_number.add(number);
                    batch_timestamp_output.add(timestamp);
                    batch_score.add(score);
                    batch_cdr_output.add(cdr);

                    map.remove(key);
                } else {
                    //LOG.warn(String.format("Inconsistent entry: source=%s; %s",
                    //       input.getSourceComponent(), e.toString()));
                }
            } else {
                Entry e = new Entry(cdr);
                e.set(src, rate);
                map.put(key, e);
            }
        }

        if (batch_number.size() >= 1)
            collector.emit(new Values(batch_number, batch_timestamp_output, batch_score, batch_cdr_output));
        collector.ack(input);
    }

    @Override
    protected Source[] getFields() {
        return new Source[]{Source.ENCR, Source.ECR};
    }
}