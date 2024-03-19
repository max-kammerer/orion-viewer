#echo $1
mkdir -p build/failures
$1 logcat > build/failures/logcat.txt &