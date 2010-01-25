function checkDir {
	 $headerMissed
	for fn in $1/*; do
	    if [ -d $fn ]; then
	        checkDir $fn
	    fi
	done
	
	for fn in $1/*.java; do
	    if [ -f $fn ]; then
	    	headerContained=`head -1 $fn |grep "/\*\*" |wc -l`
			if [ $headerContained == '0' ]; then
				echo "missing license header in file $fn"
				headerMissed="true"
			fi	
	    fi
	done
	
}


if [ $# -lt 1 ];then
	echo "Usage $0 <folder>"
	exit 1
fi

headerMissed="false"
checkDir $1
if [ $headerMissed == "true" ]; then
	exit 1
fi