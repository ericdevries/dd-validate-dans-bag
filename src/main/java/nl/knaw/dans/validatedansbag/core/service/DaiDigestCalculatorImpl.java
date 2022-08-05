package nl.knaw.dans.validatedansbag.core.service;

public class DaiDigestCalculatorImpl implements DaiDigestCalculator {
    @Override
    public char calculateChecksum(String str, int modeMax) {
        var reversed = new StringBuilder(str).reverse().toString();
        var f = 2;
        var w = 0;
        var mod = 0;

        while (mod < reversed.length()) {
            var cx = reversed.charAt(mod);
            var x = cx - 48;
            w += f * x;
            f += 1;

            if (f > modeMax) {
                f = 2;
            }

            mod += 1;
        }

        mod = w % 11;

        if (mod == 0) {
            return '0';
        }

        else {
            var c = 11 - mod;

            if (c == 10) {
                return 'X';
            }
            else {
                return (char) (c + 48);
            }
        }
    }
}
