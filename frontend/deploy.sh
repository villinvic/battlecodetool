if [ "$1" == "deploy" ]
then
	echo "WARNING: Do you really want to deploy with the game? This SHOULD NEVER BE DONE before the game is released to the world, since this means that the game specs and the visualizer become public."
	select yn in "Yes" "No"; do
		case $yn in
			Yes ) break;;
			No ) exit;;
		esac
	done
	# echo "WARNING: Do you have an up-to-date version of frontend/public/access.txt? If not, make sure to obtain it from someone who has it."
	# select yn in "Yes" "No"; do
	# 	case $yn in
	# 		Yes ) break;;
	# 		No ) exit;;
	# 	esac
	# done
	echo "Proceding with deploy!"
	mkdir public/specs
	cp ../specs public -r
        npm install
	npm run build
	rm public/specs -r
	cd build
	gsutil -m rm gs://battlecode21-frontend/**
	gsutil -m cp -r * gs://battlecode21-frontend
	cd ..
elif [ "$1" == "deploynogame" ]
then
	echo "Deploying without game!"
    npm install
	npm run buildnogame
	cd build
	gsutil -m rm gs://battlecode21-frontend/**
	gsutil -m cp -r * gs://battlecode21-frontend
	cd ..
elif [ "$1" == "clean" ]
then
	gsutil -m rm gs://battlecode21-frontend/**
else
	echo "Unsupported instruction"
fi
