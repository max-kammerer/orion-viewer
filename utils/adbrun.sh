mkdir -p build/failures
echo $1
nohup $1 logcat > build/failures/logcat.txt 2>&1 &